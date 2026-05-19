package com.testplatform.controller.apitest;

import com.testplatform.config.UserPrincipal;
import com.testplatform.dto.apitest.ApiEnvironmentDto;
import com.testplatform.entity.User;
import com.testplatform.repository.UserRepository;
import com.testplatform.service.apitest.ApiEnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-test/environments")
@RequiredArgsConstructor
public class ApiEnvironmentController {

    private final ApiEnvironmentService environmentService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ApiEnvironmentDto>> getEnvironments(@AuthenticationPrincipal UserPrincipal user) {
        User userEntity = getUserEntity(user);
        return ResponseEntity.ok(environmentService.getEnvironments(userEntity));
    }

    @PostMapping
    public ResponseEntity<ApiEnvironmentDto> create(
            @RequestBody ApiEnvironmentDto dto,
            @AuthenticationPrincipal UserPrincipal user) {
        User userEntity = getUserEntity(user);
        return ResponseEntity.ok(environmentService.create(dto, userEntity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiEnvironmentDto> update(
            @PathVariable Long id,
            @RequestBody ApiEnvironmentDto dto,
            @AuthenticationPrincipal UserPrincipal user) {
        User userEntity = getUserEntity(user);
        return ResponseEntity.ok(environmentService.update(id, dto, userEntity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        User userEntity = getUserEntity(user);
        environmentService.delete(id, userEntity);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    private User getUserEntity(UserPrincipal user) {
        if (user != null) {
            return user.getUser();
        }
        return userRepository.findByUsername("test")
                .orElseThrow(() -> new IllegalStateException("默认用户不存在"));
    }
}
