package com.testplatform.controller;

import com.testplatform.dto.UpdateUserRequest;
import com.testplatform.dto.UserDto;
import com.testplatform.entity.User;
import com.testplatform.repository.UserRepository;
import com.testplatform.service.UserService;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/auth/me")
    public ResponseEntity<?> me() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            System.err.println("[MeController] No user found in security context for /auth/me");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(toUserMap(user));
    }

    @PutMapping("/users/me")
    public ResponseEntity<?> updateMe(@RequestBody UpdateUserRequest request) {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        UserDto updated = userService.updateUser(user.getId(), request);
        return ResponseEntity.ok(toUserMap(updated));
    }

    private Map<String, Object> toUserMap(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername() != null ? user.getUsername() : "",
                "phone", user.getPhone() != null ? user.getPhone() : ""
        );
    }

    private Map<String, Object> toUserMap(UserDto user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername() != null ? user.getUsername() : "",
                "phone", user.getPhone() != null ? user.getPhone() : ""
        );
    }
}
