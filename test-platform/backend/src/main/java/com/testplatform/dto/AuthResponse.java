package com.testplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String phone;
    private Long userId;
    private Boolean isDevMode;
    private Boolean hasEnterpriseSpace;
    private Long enterpriseSpaceId;
    private String enterpriseSpaceName;
    private List<PendingInvitationDto> pendingInvitations;
}
