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

    /**
     * 用户注册接口：
     * 接收用户名、密码，创建新用户并返回包含 JWT 的登录信息。
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest req) {
        AuthResponse res = authService.register(req);
        return ResponseEntity.ok(res);
    }

    /**
     * 用户登录接口：
     * 使用用户名和密码登录，返回 JWT 令牌及用户名等信息。
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
        AuthResponse res = authService.login(req);
        return ResponseEntity.ok(res);
    }
}
