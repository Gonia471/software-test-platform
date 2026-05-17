package com.testplatform.controller.uitest;

import com.testplatform.service.uitest.AiVisionAssetService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ui-test/ai/assets")
public class AiVisionAssetController {

    private final AiVisionAssetService aiVisionAssetService;

    public AiVisionAssetController(AiVisionAssetService aiVisionAssetService) {
        this.aiVisionAssetService = aiVisionAssetService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        AiVisionAssetService.AssetInfo asset = aiVisionAssetService.store(file);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assetId", asset.assetId());
        result.put("fileName", asset.originalFilename());
        result.put("storagePath", asset.path().toString());
        result.put("previewUrl", "/api/ui-test/ai/assets/" + asset.assetId());
        return result;
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<byte[]> preview(@PathVariable String assetId) throws Exception {
        Path path = aiVisionAssetService.resolveAssetPath(assetId);
        String contentType = Files.probeContentType(path);
        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(Files.readAllBytes(path));
    }
}
