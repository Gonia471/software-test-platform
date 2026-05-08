package com.testplatform.controller.uitest;

import com.testplatform.dto.uitest.ExecutionDetailDto;
import com.testplatform.dto.uitest.ExecutionSummaryDto;
import com.testplatform.dto.uitest.StartExecutionRequest;
import com.testplatform.dto.uitest.StartExecutionResponse;
import com.testplatform.service.uitest.UiTestExecutionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/ui-test/executions")
public class UiTestExecutionController {

    private final UiTestExecutionService executionService;

    public UiTestExecutionController(UiTestExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * 最近 UI 自动化执行记录列表（用于测试报告页）。
     */
    @GetMapping
    public List<ExecutionSummaryDto> list() {
        return executionService.listRecentExecutions();
    }

    /**
     * 发起一次 UI 测试执行：
     * 指定测试用例 ID 与执行实例 ID，并附带执行选项（如 headless 等）。
     */
    @PostMapping
    public StartExecutionResponse start(@RequestBody StartExecutionRequest req) {
        return executionService.startExecution(req);
    }

    /**
     * 步骤快照（PNG）：每步执行后或失败时生成，路径为 screenshots/ui/{executionId}/step-{index}.png。
     */
    @GetMapping("/{id}/screenshots/{stepIndex}")
    public ResponseEntity<byte[]> screenshot(
            @PathVariable Long id,
            @PathVariable int stepIndex) {
        if (stepIndex < 1 || stepIndex > 9999) {
            return ResponseEntity.badRequest().build();
        }
        Path path = Paths.get("screenshots", "ui", String.valueOf(id), "step-" + stepIndex + ".png");
        path = path.toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 查询执行详情：
     * 根据执行记录 ID 返回当前状态、步骤结果等信息。
     */
    @GetMapping("/{id}")
    public ExecutionDetailDto detail(@PathVariable Long id) {
        return executionService.getDetail(id);
    }

    /**
     * 请求停止执行：
     * 根据执行记录 ID 发送停止指令，具体停止时机由执行引擎决定。
     */
    @PostMapping("/{id}/stop")
    public void stop(@PathVariable Long id) {
        executionService.requestStop(id);
    }
}

