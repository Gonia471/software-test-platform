package com.testplatform.controller;

import com.testplatform.dto.*;
import com.testplatform.entity.User;
import com.testplatform.service.OrganizationService;
import com.testplatform.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<List<OrganizationDto>> getUserOrganizations() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(organizationService.getUserOrganizations(user));
    }

    @PostMapping
    public ResponseEntity<OrganizationDto> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createOrganization(request, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDto> getOrganization(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(organizationService.getOrganization(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationDto> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(organizationService.updateOrganization(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser();
        organizationService.deleteOrganization(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/projects")
    public ResponseEntity<List<ProjectDto>> getOrganizationProjects(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(organizationService.getOrganizationProjects(id, user));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<OrganizationMemberDto>> getOrganizationMembers(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(organizationService.getOrganizationMembers(id, user));
    }

    @GetMapping("/{id}/members/me")
    public ResponseEntity<OrganizationMemberDto> getCurrentMemberInfo(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(organizationService.getMemberInfo(id, user));
    }

    @PutMapping("/{id}/members/{memberId}")
    public ResponseEntity<OrganizationMemberDto> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestBody UpdateMemberRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(organizationService.updateMemberRole(id, memberId, request, user));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId) {
        User user = SecurityUtils.getCurrentUser();
        organizationService.removeMember(id, memberId, user);
        return ResponseEntity.noContent().build();
    }
}
