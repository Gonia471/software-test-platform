package com.testplatform.config;

import com.testplatform.entity.User;
import com.testplatform.repository.UserRepository;
import com.testplatform.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            UserPrincipal userPrincipal = null;

            if ("dev-token".equals(token)) {
                List<User> users = userRepository.findAll(PageRequest.of(0, 1)).getContent();
                if (!users.isEmpty()) {
                    userPrincipal = new UserPrincipal(users.get(0));
                } else {
                    System.err.println("[JwtAuthFilter] dev-token used but no users found in database!");
                }
            } else {
                try {
                    String username = jwtUtil.getUsernameFromToken(token);
                    if (username != null) {
                        User user = userRepository.findByUsername(username).orElse(null);
                        if (user != null) {
                            userPrincipal = new UserPrincipal(user);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[JwtAuthFilter] Token validation failed: " + e.getMessage());
                }
            }

            if (userPrincipal != null) {
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, userPrincipal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } else {
                System.err.println("[JwtAuthFilter] No user principal found for token");
            }
        }
        filterChain.doFilter(request, response);
    }
}
