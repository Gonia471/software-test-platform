package com.testplatform.controller;

import com.testplatform.dto.AuthResponse;
import com.testplatform.dto.LoginRequest;
import com.testplatform.dto.RegisterRequest;
import com.testplatform.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest req) {
        AuthResponse res = authService.register(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
        AuthResponse res = authService.login(req);
        return ResponseEntity.ok(res);
    }
}
