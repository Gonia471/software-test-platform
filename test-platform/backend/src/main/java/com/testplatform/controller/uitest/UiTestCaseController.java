package com.testplatform.controller.uitest;

import com.testplatform.dto.uitest.CreateOrUpdateCaseRequest;
import com.testplatform.dto.uitest.UiTestCaseDto;
import com.testplatform.service.uitest.UiTestCaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ui-test/test-cases")
public class UiTestCaseController {

    private final UiTestCaseService testCaseService;

    public UiTestCaseController(UiTestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    /** 创建 UI 测试用例 */
    @PostMapping
    public ResponseEntity<UiTestCaseDto> create(@RequestBody CreateOrUpdateCaseRequest req) {
        UiTestCaseDto dto = testCaseService.create(req);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UiTestCaseDto> update(@PathVariable Long id, @RequestBody CreateOrUpdateCaseRequest req) {
        UiTestCaseDto dto = testCaseService.update(id, req);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UiTestCaseDto> get(@PathVariable Long id) {
        UiTestCaseDto dto = testCaseService.getDetail(id);
        return ResponseEntity.ok(dto);
    }

    /** 查询全部测试用例列表（仅基本信息，不含 steps 明细） */
    @GetMapping
    public ResponseEntity<List<UiTestCaseDto>> list() {
        List<UiTestCaseDto> list = testCaseService.listAll();
        return ResponseEntity.ok(list);
    }

    /** 删除指定 ID 的测试用例 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        testCaseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

