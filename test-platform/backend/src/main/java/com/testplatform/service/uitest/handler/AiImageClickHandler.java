package com.testplatform.service.uitest.handler;

import com.testplatform.service.uitest.AiActionPlannerService;
import com.testplatform.service.uitest.AiVisionAssetService;
import com.testplatform.service.uitest.ImageTemplateMatchService;
import com.testplatform.service.uitest.model.AiPlannedAction;
import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 图像识别点击：当前阶段仅做演示实现，实际识别逻辑后续可接入 OpenCV/外部 AI 服务。
 */
@Component
public class AiImageClickHandler implements StepHandler {

    private final AiActionPlannerService aiActionPlannerService;
    private final AiVisionAssetService aiVisionAssetService;
    private final ImageTemplateMatchService imageTemplateMatchService;

    public AiImageClickHandler(AiActionPlannerService aiActionPlannerService,
                               AiVisionAssetService aiVisionAssetService,
                               ImageTemplateMatchService imageTemplateMatchService) {
        this.aiActionPlannerService = aiActionPlannerService;
        this.aiVisionAssetService = aiVisionAssetService;
        this.imageTemplateMatchService = imageTemplateMatchService;
    }

    @Override
    public boolean supports(StepDefinition step) {
        return "ai".equalsIgnoreCase(step.getType()) && "aiImageClick".equals(step.getAction());
    }

    @Override
    public StepResult execute(StepDefinition step, ExecutionContext ctx) {
        WebDriver driver = ctx.getDriver();
        Map<String, Object> params = step.getParameters();
        String assetId = (String) params.getOrDefault("assetId", "");
        String assetName = (String) params.getOrDefault("assetName", "");
        String imagePath = (String) params.getOrDefault("imagePath", "");
        String instruction = (String) params.getOrDefault("instruction", "");
        String sourceType = String.valueOf(params.getOrDefault("sourceType", "upload"));
        String mode = String.valueOf(params.getOrDefault("mode", "crop"));
        double threshold = resolveThreshold(params.get("threshold"));

        if (assetId != null && !assetId.isBlank()) {
            MatchExecution execution = matchAndClick(driver, assetId, mode, params, threshold);
            StepResult result = StepResult.success("AI图像点击执行成功: assetId=" + assetId
                    + (assetName == null || assetName.isBlank() ? "" : " | assetName=" + assetName)
                    + " | mode=" + mode
                    + " | source=" + sourceType
                    + " | center=(" + execution.match().centerX() + "," + execution.match().centerY() + ")"
                    + " | rect=(" + execution.match().left() + "," + execution.match().top() + "," + execution.match().width() + "x" + execution.match().height() + ")"
                    + " | score=" + String.format("%.3f", execution.match().score())
                    + " | threshold=" + String.format("%.2f", threshold)
                    + " | strategy=template-match");
            String annotatedShot = saveAnnotatedScreenshot(ctx, execution.screenshot(), execution.match());
            if (annotatedShot != null) {
                result.setScreenshotPath(annotatedShot);
            }
            return result;
        }

        AiPlannedAction action = aiActionPlannerService.planImageClick(imagePath, instruction, driver);
        performClick(action, driver);
        return StepResult.success("AI图像点击执行成功: imagePath=" + imagePath
                + " | reason=" + (action.getReason() == null ? "-" : action.getReason()));
    }

    private MatchExecution matchAndClick(WebDriver driver,
                                         String assetId,
                                         String mode,
                                         Map<String, Object> params,
                                         double threshold) {
        try {
            if ("crop".equalsIgnoreCase(mode) && !hasValidCropBox(params)) {
                throw new IllegalArgumentException("当前为框选区域点击，请先在图片预览中框选目标区域");
            }
            BufferedImage screenshot = captureScreenshot(driver);
            ImageTemplateMatchService.MatchResult match = imageTemplateMatchService.match(
                    aiVisionAssetService.resolveAssetPath(assetId),
                    mode,
                    getBox(params),
                    screenshot,
                    threshold
            );
            clickAt(driver, screenshot, match.centerX(), match.centerY());
            return new MatchExecution(screenshot, match);
        } catch (Exception e) {
            throw new IllegalStateException("图像识别点击失败，assetId=" + assetId
                    + ", mode=" + mode
                    + ", reason=" + shortError(e), e);
        }
    }

    private String saveAnnotatedScreenshot(ExecutionContext ctx,
                                           BufferedImage screenshot,
                                           ImageTemplateMatchService.MatchResult match) {
        try {
            if (screenshot == null) {
                return null;
            }

            Graphics2D graphics = screenshot.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(new Color(239, 68, 68));
                graphics.setStroke(new BasicStroke(3f));
                graphics.drawRect(match.left(), match.top(), Math.max(1, match.width()), Math.max(1, match.height()));

                int centerX = match.centerX();
                int centerY = match.centerY();
                graphics.setColor(new Color(34, 197, 94));
                graphics.fillOval(centerX - 5, centerY - 5, 10, 10);
                graphics.drawLine(centerX - 12, centerY, centerX + 12, centerY);
                graphics.drawLine(centerX, centerY - 12, centerX, centerY + 12);

                graphics.setColor(new Color(15, 23, 42, 210));
                graphics.fillRoundRect(
                        Math.max(8, match.left()),
                        Math.max(8, match.top() - 28),
                        220,
                        22,
                        10,
                        10
                );
                graphics.setColor(Color.WHITE);
                graphics.drawString(
                        "template-match score=" + String.format("%.3f", match.score()),
                        Math.max(16, match.left() + 8),
                        Math.max(24, match.top() - 12)
                );
            } finally {
                graphics.dispose();
            }

            Path target = ctx.getScreenshotRoot().resolve("step-" + resolveCurrentStepIndex(ctx) + ".png");
            Files.createDirectories(target.getParent());
            ImageIO.write(screenshot, "png", target.toFile());
            return target.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private int resolveCurrentStepIndex(ExecutionContext ctx) {
        Object value = ctx.getVariables().get("currentStepIndex");
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 1;
        }
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    private String shortError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getBox(Map<String, Object> params) {
        Object box = params.get("box");
        if (box instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private BufferedImage captureScreenshot(WebDriver driver) throws Exception {
        if (!(driver instanceof TakesScreenshot ts)) {
            throw new IllegalStateException("当前驱动不支持截图");
        }
        File file = ts.getScreenshotAs(OutputType.FILE);
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IllegalStateException("页面截图读取失败");
        }
        return image;
    }

    private boolean hasValidCropBox(Map<String, Object> params) {
        Map<String, Object> box = getBox(params);
        return toDouble(box.get("widthRatio")) > 0D && toDouble(box.get("heightRatio")) > 0D;
    }

    private void clickAt(WebDriver driver, BufferedImage screenshot, int screenshotX, int screenshotY) {
        if (!(driver instanceof JavascriptExecutor js)) {
            throw new IllegalStateException("当前驱动不支持坐标点击");
        }

        Number viewportWidth = (Number) js.executeScript("return window.innerWidth;");
        Number viewportHeight = (Number) js.executeScript("return window.innerHeight;");
        double widthScale = viewportWidth.doubleValue() / screenshot.getWidth();
        double heightScale = viewportHeight.doubleValue() / screenshot.getHeight();
        int viewportX = (int) Math.round(screenshotX * widthScale);
        int viewportY = (int) Math.round(screenshotY * heightScale);

        js.executeScript(
                "const x=arguments[0], y=arguments[1];"
                        + "const el=document.elementFromPoint(x,y);"
                        + "if(!el){throw new Error('未找到坐标对应元素');}"
                        + "el.dispatchEvent(new MouseEvent('mousemove',{bubbles:true,clientX:x,clientY:y}));"
                        + "el.dispatchEvent(new MouseEvent('mousedown',{bubbles:true,clientX:x,clientY:y}));"
                        + "el.dispatchEvent(new MouseEvent('mouseup',{bubbles:true,clientX:x,clientY:y}));"
                        + "el.click();",
                viewportX,
                viewportY
        );
    }

    private double resolveThreshold(Object value) {
        if (value == null) {
            return 0.82D;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.82D;
        }
    }

    private void performClick(AiPlannedAction action, WebDriver driver) {
        if (action.getLocatorType() != null && action.getLocatorValue() != null) {
            WebElement element = driver.findElement(LocatorSupport.buildLocator(Map.of(
                    "locatorType", action.getLocatorType(),
                    "locatorValue", action.getLocatorValue()
            )));
            element.click();
            return;
        }
        if (action.getX() != null && action.getY() != null && driver instanceof JavascriptExecutor js) {
            js.executeScript(
                    "const el=document.elementFromPoint(arguments[0],arguments[1]); if(el){el.click();}",
                    action.getX(), action.getY()
            );
            return;
        }
        throw new IllegalArgumentException("AI 未返回可执行点击目标");
    }

    private record MatchExecution(BufferedImage screenshot, ImageTemplateMatchService.MatchResult match) {
    }
}
