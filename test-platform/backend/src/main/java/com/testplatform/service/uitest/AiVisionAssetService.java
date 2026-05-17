package com.testplatform.service.uitest;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class AiVisionAssetService {

    private static final Path ROOT = Paths.get("uploads", "ui-ai");

    public AssetInfo store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传图片为空");
        }

        Files.createDirectories(ROOT);
        String assetId = "vision-" + UUID.randomUUID().toString().replace("-", "");
        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        String fileName = assetId + extension;
        Path target = ROOT.resolve(fileName).normalize().toAbsolutePath();
        if (!target.startsWith(ROOT.toAbsolutePath().normalize())) {
            throw new IllegalStateException("非法文件路径");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return new AssetInfo(assetId, file.getOriginalFilename(), target);
    }

    public Path resolveAssetPath(String assetId) throws IOException {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId 不能为空");
        }
        Files.createDirectories(ROOT);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(ROOT, assetId + ".*")) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    return path.toAbsolutePath().normalize();
                }
            }
        }
        throw new IllegalArgumentException("图片资源不存在: " + assetId);
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String lowerName = originalFilename == null ? "" : originalFilename.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".png")) return ".png";
        if (lowerName.endsWith(".jpg")) return ".jpg";
        if (lowerName.endsWith(".jpeg")) return ".jpeg";
        if (lowerName.endsWith(".gif")) return ".gif";
        if (lowerName.endsWith(".webp")) return ".webp";

        String lowerType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (lowerType.contains("png")) return ".png";
        if (lowerType.contains("jpeg")) return ".jpeg";
        if (lowerType.contains("jpg")) return ".jpg";
        if (lowerType.contains("gif")) return ".gif";
        if (lowerType.contains("webp")) return ".webp";
        return ".png";
    }

    public record AssetInfo(String assetId, String originalFilename, Path path) {
    }
}
