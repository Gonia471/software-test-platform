package com.testplatform.service;

import com.testplatform.dto.AuthResponse;
import com.testplatform.dto.LoginRequest;
import com.testplatform.dto.RegisterRequest;
import com.testplatform.entity.User;
import com.testplatform.repository.UserRepository;
import com.testplatform.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User(req.getUsername(), passwordEncoder.encode(req.getPassword()));
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordEncrypted())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }
}
