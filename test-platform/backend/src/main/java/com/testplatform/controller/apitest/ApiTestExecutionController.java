package com.testplatform.controller.apitest;

import com.testplatform.config.UserPrincipal;
import com.testplatform.dto.apitest.ApiTestExecutionDetailDto;
import com.testplatform.dto.apitest.ApiTestExecutionSummaryDto;
import com.testplatform.dto.apitest.SaveExecutionRequest;
import com.testplatform.service.apitest.ApiTestExecutionService;
import com.testplatform.service.apitest.ApiTestExecutionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-test/executions")
@RequiredArgsConstructor
public class ApiTestExecutionController {

    private final ApiTestExecutionEngine executionEngine;
    private final ApiTestExecutionService executionService;

    @PostMapping("/execute/{collectionId}")
    public ApiTestExecutionEngine.ExecutionResult execute(
            @PathVariable Long collectionId,
            @AuthenticationPrincipal UserPrincipal user) {
        return executionEngine.execute(collectionId, user.getUser());
    }

    @GetMapping
    public ResponseEntity<List<ApiTestExecutionSummaryDto>> list(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(executionService.listRecentExecutions(user.getId(), limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiTestExecutionDetailDto> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(executionService.getDetail(id, user.getId()));
    }

    @PostMapping
    public ResponseEntity<ApiTestExecutionSummaryDto> saveExecution(
            @RequestBody SaveExecutionRequest request,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(executionService.saveExecution(request, user.getUser()));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(executionService.getStatistics(user.getId()));
    }
}
