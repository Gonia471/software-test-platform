package com.testplatform.controller;

import com.testplatform.dto.DashboardOverviewDto;
import com.testplatform.entity.User;
import com.testplatform.service.DashboardService;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDto> getOverview(
            @RequestParam(value = "organizationId", required = false) Long organizationId
    ) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(dashboardService.getOverview(organizationId, user));
    }
}
