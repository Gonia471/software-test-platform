package com.testplatform.controller;

import com.testplatform.config.UserPrincipal;
import com.testplatform.dto.CreateInvitationRequest;
import com.testplatform.dto.InvitationCheckResponse;
import com.testplatform.dto.InvitationDto;
import com.testplatform.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping("/check")
    public ResponseEntity<InvitationCheckResponse> checkInvitation(@RequestParam String phone) {
        InvitationCheckResponse response = invitationService.checkInvitationByPhone(phone);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orgs/{orgId}")
    public ResponseEntity<InvitationDto> createInvitation(
            @PathVariable Long orgId,
            @RequestBody @Valid CreateInvitationRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        InvitationDto invitation = invitationService.createInvitation(orgId, request, user.getUser());
        return ResponseEntity.ok(invitation);
    }

    @GetMapping("/orgs/{orgId}")
    public ResponseEntity<List<InvitationDto>> getOrgInvitations(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal user) {
        List<InvitationDto> invitations = invitationService.getOrganizationInvitations(orgId, user.getUser());
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/use/{code}")
    public ResponseEntity<Void> useInvitation(
            @PathVariable String code,
            @AuthenticationPrincipal UserPrincipal user) {
        invitationService.useInvitation(code, user.getUser());
        return ResponseEntity.ok().build();
    }
}