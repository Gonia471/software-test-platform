package com.testplatform.service;

import com.testplatform.dto.UserDto;
import com.testplatform.dto.UpdateUserRequest;
import com.testplatform.entity.User;
import com.testplatform.repository.UserRepository;
import com.testplatform.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return toDto(user);
    }

    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return toDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            user.setUsername(request.getUsername());
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            String normalizedPhone = PhoneUtils.normalizeAndValidate(request.getPhone());
            if (!normalizedPhone.equals(user.getPhone())) {
                if (userRepository.findByPhone(normalizedPhone).isPresent()) {
                    throw new IllegalArgumentException("该手机号已被使用");
                }
            }
            user.setPhone(normalizedPhone);
        }

        user = userRepository.save(user);
        return toDto(user);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .build();
    }
}
