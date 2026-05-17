package com.testplatform.controller;

import com.testplatform.dto.EnterpriseSpaceDto;
import com.testplatform.dto.EnterpriseSpaceMemberDto;
import com.testplatform.entity.User;
import com.testplatform.service.EnterpriseSpaceService;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enterprise-space")
@RequiredArgsConstructor
public class EnterpriseSpaceController {

    private final EnterpriseSpaceService enterpriseSpaceService;

    @GetMapping("/current")
    public ResponseEntity<EnterpriseSpaceDto> getCurrentSpace() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(enterpriseSpaceService.getCurrentSpaceDto(user));
    }

    @GetMapping("/members")
    public ResponseEntity<List<EnterpriseSpaceMemberDto>> getCurrentSpaceMembers() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(enterpriseSpaceService.getCurrentSpaceMembers(user));
    }
}
