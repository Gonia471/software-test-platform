package com.testplatform.service.uitest;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Service
public class ImageTemplateMatchService {

    private static final double[] SCALES = {0.85D, 1.0D, 1.15D};

    public MatchResult match(Path assetPath,
                             String mode,
                             Map<String, Object> box,
                             BufferedImage screenshot,
                             double threshold) throws IOException {
        if (screenshot == null) {
            throw new IllegalArgumentException("当前页面截图为空");
        }

        BufferedImage source = ImageIO.read(assetPath.toFile());
        if (source == null) {
            throw new IllegalArgumentException("无法读取图片资源: " + assetPath);
        }

        BufferedImage template = buildTemplate(source, mode, box);
        if (template.getWidth() < 4 || template.getHeight() < 4) {
            throw new IllegalArgumentException("模板区域过小，无法进行匹配");
        }

        GrayImage screenshotGray = GrayImage.from(screenshot);
        MatchResult best = null;
        for (double scale : SCALES) {
            BufferedImage scaledTemplate = scale == 1.0D ? template : resize(template, scale);
            if (scaledTemplate.getWidth() < 4 || scaledTemplate.getHeight() < 4) {
                continue;
            }
            if (scaledTemplate.getWidth() > screenshot.getWidth()
                    || scaledTemplate.getHeight() > screenshot.getHeight()) {
                continue;
            }

            GrayImage templateGray = GrayImage.from(scaledTemplate);
            MatchResult candidate = searchBest(templateGray, screenshotGray, threshold);
            if (candidate != null && (best == null || candidate.score() > best.score())) {
                best = candidate;
            }
        }

        if (best == null || best.score() < threshold) {
            throw new IllegalStateException("未找到足够相似的图片目标，bestScore="
                    + (best == null ? "-" : String.format("%.3f", best.score()))
                    + ", threshold=" + String.format("%.3f", threshold));
        }
        return best;
    }

    private MatchResult searchBest(GrayImage template, GrayImage screenshot, double threshold) {
        int coarseStep = Math.max(3, Math.min(template.width(), template.height()) / 18);
        int sampleStep = Math.max(1, Math.min(template.width(), template.height()) / 14);
        MatchResult coarseBest = null;
        for (int y = 0; y <= screenshot.height() - template.height(); y += coarseStep) {
            for (int x = 0; x <= screenshot.width() - template.width(); x += coarseStep) {
                double score = score(template, screenshot, x, y, sampleStep);
                if (coarseBest == null || score > coarseBest.score()) {
                    coarseBest = new MatchResult(x, y, template.width(), template.height(), score);
                }
            }
        }

        if (coarseBest == null) {
            return null;
        }

        int refineRadius = Math.max(4, coarseStep * 2);
        MatchResult refinedBest = coarseBest;
        int startY = Math.max(0, coarseBest.top() - refineRadius);
        int endY = Math.min(screenshot.height() - template.height(), coarseBest.top() + refineRadius);
        int startX = Math.max(0, coarseBest.left() - refineRadius);
        int endX = Math.min(screenshot.width() - template.width(), coarseBest.left() + refineRadius);
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                double score = score(template, screenshot, x, y, 1);
                if (score > refinedBest.score()) {
                    refinedBest = new MatchResult(x, y, template.width(), template.height(), score);
                }
            }
        }
        return refinedBest.score() >= threshold ? refinedBest : coarseBest;
    }

    private double score(GrayImage template, GrayImage screenshot, int offsetX, int offsetY, int step) {
        double totalDiff = 0D;
        int samples = 0;
        for (int y = 0; y < template.height(); y += step) {
            for (int x = 0; x < template.width(); x += step) {
                int templateValue = template.grayAt(x, y);
                int screenshotValue = screenshot.grayAt(offsetX + x, offsetY + y);
                totalDiff += Math.abs(templateValue - screenshotValue);
                samples++;
            }
        }
        if (samples == 0) {
            return 0D;
        }
        double normalized = totalDiff / (samples * 255D);
        return 1D - normalized;
    }

    private BufferedImage buildTemplate(BufferedImage source, String mode, Map<String, Object> box) {
        if (!"crop".equalsIgnoreCase(mode)) {
            return source;
        }
        double xRatio = toRatio(box.get("xRatio"));
        double yRatio = toRatio(box.get("yRatio"));
        double widthRatio = toRatio(box.get("widthRatio"));
        double heightRatio = toRatio(box.get("heightRatio"));
        if (widthRatio <= 0D || heightRatio <= 0D) {
            throw new IllegalArgumentException("框选区域无效，请先框选目标区域");
        }

        int x = clamp((int) Math.round(xRatio * source.getWidth()), 0, source.getWidth() - 1);
        int y = clamp((int) Math.round(yRatio * source.getHeight()), 0, source.getHeight() - 1);
        int width = clamp((int) Math.round(widthRatio * source.getWidth()), 1, source.getWidth() - x);
        int height = clamp((int) Math.round(heightRatio * source.getHeight()), 1, source.getHeight() - y);
        return source.getSubimage(x, y, width, height);
    }

    private double toRatio(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private BufferedImage resize(BufferedImage source, double scale) {
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record MatchResult(int left, int top, int width, int height, double score) {
        public int centerX() {
            return left + width / 2;
        }

        public int centerY() {
            return top + height / 2;
        }
    }

    private record GrayImage(int width, int height, int[] data) {
        static GrayImage from(BufferedImage image) {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] gray = new int[width * height];
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    gray[index++] = (r * 30 + g * 59 + b * 11) / 100;
                }
            }
            return new GrayImage(width, height, gray);
        }

        int grayAt(int x, int y) {
            return data[y * width + x];
        }
    }
}
