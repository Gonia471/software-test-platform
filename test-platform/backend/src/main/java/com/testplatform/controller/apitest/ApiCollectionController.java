package com.testplatform.controller.apitest;

import com.testplatform.config.UserPrincipal;
import com.testplatform.dto.apitest.ApiCollectionDto;
import com.testplatform.entity.User;
import com.testplatform.repository.UserRepository;
import com.testplatform.service.apitest.ApiCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-test/collections")
@RequiredArgsConstructor
public class ApiCollectionController {

    private final ApiCollectionService collectionService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ApiCollectionDto>> getCollections(@AuthenticationPrincipal UserPrincipal user) {
        Long userId = getUserId(user);
        return ResponseEntity.ok(collectionService.getCollections(userId));
    }

    @PostMapping
    public ResponseEntity<ApiCollectionDto> create(
            @RequestBody ApiCollectionDto dto,
            @AuthenticationPrincipal UserPrincipal user) {
        Long userId = getUserId(user);
        User userEntity = getUserEntity(user);
        return ResponseEntity.ok(collectionService.create(dto, userEntity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiCollectionDto> update(
            @PathVariable Long id,
            @RequestBody ApiCollectionDto dto,
            @AuthenticationPrincipal UserPrincipal user) {
        Long userId = getUserId(user);
        User userEntity = getUserEntity(user);
        return ResponseEntity.ok(collectionService.update(id, dto, userEntity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        Long userId = getUserId(user);
        User userEntity = getUserEntity(user);
        collectionService.delete(id, userEntity);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    private Long getUserId(UserPrincipal user) {
        if (user != null) {
            return user.getId();
        }
        User defaultUser = userRepository.findByUsername("test").orElse(null);
        if (defaultUser != null) {
            return defaultUser.getId();
        }
        throw new IllegalStateException("无法获取用户信息");
    }

    private User getUserEntity(UserPrincipal user) {
        if (user != null) {
            return user.getUser();
        }
        return userRepository.findByUsername("test")
                .orElseThrow(() -> new IllegalStateException("默认用户不存在"));
    }
}