package com.testplatform.controller.uitest;

import com.testplatform.dto.uitest.CreateOrUpdateCaseRequest;
import com.testplatform.dto.uitest.UiTestCaseDto;
import com.testplatform.entity.User;
import com.testplatform.service.uitest.UiTestCaseService;
import com.testplatform.service.uitest.XPathPreviewService;
import com.testplatform.service.uitest.handler.LocatorSupport;
import com.testplatform.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ui-test")
public class UiTestCaseController {

    private final UiTestCaseService testCaseService;
    private final XPathPreviewService xpathPreviewService;

    public UiTestCaseController(UiTestCaseService testCaseService, XPathPreviewService xpathPreviewService) {
        this.testCaseService = testCaseService;
        this.xpathPreviewService = xpathPreviewService;
    }

    /** 创建 UI 测试用例 */
    @PostMapping("/test-cases")
    public ResponseEntity<UiTestCaseDto> create(@RequestBody CreateOrUpdateCaseRequest req) {
        User user = SecurityUtils.getCurrentUser();
        UiTestCaseDto dto = testCaseService.create(req, user);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/test-cases/{id}")
    public ResponseEntity<UiTestCaseDto> update(@PathVariable Long id, @RequestBody CreateOrUpdateCaseRequest req) {
        User user = SecurityUtils.getCurrentUser();
        UiTestCaseDto dto = testCaseService.update(id, req, user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/test-cases/{id}")
    public ResponseEntity<UiTestCaseDto> get(@PathVariable Long id) {
        UiTestCaseDto dto = testCaseService.getDetail(id);
        return ResponseEntity.ok(dto);
    }

    /** 查询全部测试用例列表（仅基本信息，不含 steps 明细） */
    @GetMapping("/test-cases")
    public ResponseEntity<List<UiTestCaseDto>> list() {
        List<UiTestCaseDto> list = testCaseService.listAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/test-cases/organization/{orgId}")
    public ResponseEntity<List<UiTestCaseDto>> listByOrganization(@PathVariable Long orgId) {
        User user = SecurityUtils.getCurrentUser();
        List<UiTestCaseDto> list = testCaseService.listByOrganization(orgId, user);
        return ResponseEntity.ok(list);
    }

    /** 删除指定 ID 的测试用例 */
    @DeleteMapping("/test-cases/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        testCaseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** XPath 优化预览 */
    @GetMapping("/xpath/preview")
    public ResponseEntity<Map<String, String>> previewXpath(@RequestParam String xpath) {
        Map<String, String> result = LocatorSupport.analyzeXpath(xpath);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/xpath/preview")
    public ResponseEntity<Map<String, String>> previewXpathWithContext(@RequestBody XPathPreviewRequest request) {
        Map<String, String> result = xpathPreviewService.preview(request.xpath(), request.pageUrl());
        return ResponseEntity.ok(result);
    }

    private record XPathPreviewRequest(String xpath, String pageUrl) {
    }
}
