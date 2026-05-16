package com.testplatform.service;

import com.testplatform.entity.Organization;
import com.testplatform.entity.OrganizationMember;
import com.testplatform.entity.User;
import com.testplatform.repository.OrganizationMemberRepository;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationPermissionService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;

    public List<Long> getAccessibleOrganizationIds(User user) {
        if (SecurityUtils.isDevMode()) {
            return organizationRepository.findAll().stream()
                    .map(Organization::getId)
                    .collect(Collectors.toList());
        }

        List<OrganizationMember> memberships = memberRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());

        if (memberships.isEmpty()) {
            return List.of();
        }

        boolean hasSpaceAdmin = memberships.stream()
                .anyMatch(OrganizationMember::isSpaceAdmin);

        if (hasSpaceAdmin) {
            return organizationRepository.findAll().stream()
                    .map(Organization::getId)
                    .collect(Collectors.toList());
        }

        return memberships.stream()
                .map(m -> m.getOrganization().getId())
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean canViewOrganization(Long orgId, User user) {
        if (SecurityUtils.isDevMode()) {
            return true;
        }

        return memberRepository.findByOrganizationIdAndUserId(orgId, user.getId())
                .isPresent();
    }

    public boolean canManageOrganization(Long orgId, User user) {
        if (SecurityUtils.isDevMode()) {
            return true;
        }

        return memberRepository.findByOrganizationIdAndUserId(orgId, user.getId())
                .map(OrganizationMember::isOrgAdmin)
                .orElse(false);
    }

    public boolean canViewAllOrganizations(User user) {
        if (SecurityUtils.isDevMode()) {
            return true;
        }

        List<OrganizationMember> memberships = memberRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());

        return memberships.stream()
                .anyMatch(OrganizationMember::isSpaceAdmin);
    }

    public List<Long> getManageableOrganizationIds(User user) {
        if (SecurityUtils.isDevMode()) {
            return organizationRepository.findAll().stream()
                    .map(Organization::getId)
                    .collect(Collectors.toList());
        }

        return memberRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .filter(OrganizationMember::isOrgAdmin)
                .map(m -> m.getOrganization().getId())
                .distinct()
                .collect(Collectors.toList());
    }
}