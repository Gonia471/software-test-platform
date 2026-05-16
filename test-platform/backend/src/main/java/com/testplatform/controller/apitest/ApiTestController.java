package com.testplatform.controller.apitest;

import com.testplatform.config.UserPrincipal;
import com.testplatform.entity.User;
import com.testplatform.repository.UserRepository;
import com.testplatform.service.apitest.ApiTestExecutionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/api-test")
@RequiredArgsConstructor
public class ApiTestController {

    private final ApiTestExecutionEngine executionEngine;
    private final UserRepository userRepository;

    @PostMapping("/execute/{collectionId}")
    public ApiTestExecutionEngine.ExecutionResult execute(
            @PathVariable Long collectionId,
            @AuthenticationPrincipal UserPrincipal user) {
        User userEntity = getUserEntity(user);
        return executionEngine.execute(collectionId, userEntity);
    }

    private User getUserEntity(UserPrincipal user) {
        if (user != null) {
            return user.getUser();
        }
        return userRepository.findByUsername("test")
                .orElseThrow(() -> new RuntimeException("默认用户不存在"));
    }
}