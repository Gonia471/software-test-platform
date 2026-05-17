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
public class InvitationDto {
    private Long id;
    private Long enterpriseSpaceId;
    private String enterpriseSpaceName;
    private Long organizationId;
    private String organizationName;
    private String invitedPhone;
    private Boolean used;
    private String usedByUsername;
    private String invitedByUsername;
    private Instant createdAt;
    private Instant expiredAt;
}
