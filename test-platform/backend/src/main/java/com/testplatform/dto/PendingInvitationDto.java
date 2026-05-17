package com.testplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingInvitationDto {
    private Long invitationId;
    private Long enterpriseSpaceId;
    private String enterpriseSpaceName;
    private Long organizationId;
    private String organizationName;
    private String invitedPhone;
    private String inviterName;
    private Instant invitedAt;
}
