package com.testplatform.controller;

import com.testplatform.dto.AuthResponse;
import com.testplatform.dto.LoginRequest;
import com.testplatform.dto.RegisterRequest;
import com.testplatform.entity.User;
import com.testplatform.service.AuthService;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-with-org")
    public ResponseEntity<AuthResponse> registerWithOrg(@RequestBody RegisterRequest req) {
        AuthResponse res = authService.registerWithOrg(req.getPhone(), req.getOrgName(), req.getDescription());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login-with-code")
    public ResponseEntity<AuthResponse> loginWithCode(@RequestBody LoginRequest req) {
        AuthResponse res = authService.loginWithCode(req.getPhone());
        return ResponseEntity.ok(res);
    }
}
