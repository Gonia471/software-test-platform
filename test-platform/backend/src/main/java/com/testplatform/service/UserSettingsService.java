package com.testplatform.service;

import com.testplatform.dto.UserSettingsDto;
import com.testplatform.entity.UserSettings;
import com.testplatform.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsDto getSettings(Long userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        return convertToDto(settings);
    }

    @Transactional
    public UserSettingsDto updateSettings(Long userId, UserSettingsDto dto) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        if (dto.getThemeMode() != null) {
            settings.setThemeMode(dto.getThemeMode());
        }
        if (dto.getNotifyOnComplete() != null) {
            settings.setNotifyOnComplete(dto.getNotifyOnComplete());
        }
        if (dto.getLanguage() != null) {
            settings.setLanguage(dto.getLanguage());
        }
        if (dto.getPageSize() != null) {
            settings.setPageSize(dto.getPageSize());
        }

        return convertToDto(userSettingsRepository.save(settings));
    }

    private UserSettings createDefaultSettings(Long userId) {
        UserSettings settings = new UserSettings(userId);
        return userSettingsRepository.save(settings);
    }

    private UserSettingsDto convertToDto(UserSettings settings) {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setUserId(settings.getUserId());
        dto.setThemeMode(settings.getThemeMode());
        dto.setNotifyOnComplete(settings.getNotifyOnComplete());
        dto.setLanguage(settings.getLanguage());
        dto.setPageSize(settings.getPageSize());
        return dto;
    }
}