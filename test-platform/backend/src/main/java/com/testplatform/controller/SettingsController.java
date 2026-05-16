package com.testplatform.controller;

import com.testplatform.dto.OrgConfigDto;
import com.testplatform.dto.UserSettingsDto;
import com.testplatform.service.OrgConfigService;
import com.testplatform.service.UserSettingsService;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserSettingsService userSettingsService;
    private final OrgConfigService orgConfigService;

    @GetMapping("/user")
    public ResponseEntity<UserSettingsDto> getUserSettings() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userSettingsService.getSettings(userId));
    }

    @PutMapping("/user")
    public ResponseEntity<UserSettingsDto> updateUserSettings(@RequestBody UserSettingsDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userSettingsService.updateSettings(userId, dto));
    }

    @GetMapping("/org")
    public ResponseEntity<OrgConfigDto> getOrgConfig(@RequestParam Long organizationId) {
        return ResponseEntity.ok(orgConfigService.getConfig(organizationId));
    }

    @PutMapping("/org")
    public ResponseEntity<OrgConfigDto> updateOrgConfig(
            @RequestParam Long organizationId,
            @RequestBody OrgConfigDto dto) {
        return ResponseEntity.ok(orgConfigService.updateConfig(organizationId, dto));
    }
}