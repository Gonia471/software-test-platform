package com.testplatform.controller.apitest;

import com.testplatform.config.UserPrincipal;
import com.testplatform.entity.User;
import com.testplatform.repository.UserRepository;
import com.testplatform.service.apitest.ApiTestExecutionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-test/executions")
@RequiredArgsConstructor
public class ApiTestExecutionController {

    private final ApiTestExecutionEngine executionEngine;
    private final UserRepository userRepository;

    @PostMapping("/execute/{collectionId}")
    public ApiTestExecutionEngine.ExecutionResult execute(
            @PathVariable Long collectionId,
            @AuthenticationPrincipal UserPrincipal user) {
        User userEntity = getUserEntity(user);
        return executionEngine.execute(collectionId, userEntity);
    }

    @GetMapping
    public List<?> list(@AuthenticationPrincipal UserPrincipal user) {
        return List.of();
    }

    @GetMapping("/{id}")
    public Object getDetail(@PathVariable Long id) {
        return Map.of();
    }

    private User getUserEntity(UserPrincipal user) {
        if (user != null) {
            return user.getUser();
        }
        return userRepository.findByUsername("test")
                .orElseThrow(() -> new RuntimeException("默认用户不存在"));
    }
}
