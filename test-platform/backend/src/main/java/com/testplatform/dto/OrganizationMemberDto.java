package com.testplatform.dto;

import com.testplatform.entity.OrganizationMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMemberDto {
    private Long id;
    private Long userId;
    private String username;
    private String phone;
    private OrganizationMember.Role role;
    private Boolean canRead;
    private Boolean canWrite;
    private Instant joinedAt;
}