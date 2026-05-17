package com.testplatform.service;

import com.testplatform.entity.Organization;
import com.testplatform.entity.OrganizationMember;
import com.testplatform.entity.User;
import com.testplatform.repository.EnterpriseSpaceMemberRepository;
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
    private final EnterpriseSpaceService enterpriseSpaceService;
    private final EnterpriseSpaceMemberRepository enterpriseSpaceMemberRepository;

    public List<Long> getAccessibleOrganizationIds(User user) {
        if (SecurityUtils.isDevMode()) {
            return organizationRepository.findAll().stream()
                    .map(Organization::getId)
                    .collect(Collectors.toList());
        }

        return organizationRepository.findAllByMember(user).stream()
                .map(Organization::getId)
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

        try {
            Long enterpriseSpaceId = organizationRepository.findById(orgId)
                    .orElseThrow()
                    .getEnterpriseSpace()
                    .getId();
            boolean isSpaceAdmin = enterpriseSpaceMemberRepository
                    .findByEnterpriseSpaceIdAndUserId(enterpriseSpaceId, user.getId())
                    .map(m -> m.isSpaceAdmin())
                    .orElse(false);
            if (isSpaceAdmin) {
                return true;
            }
        } catch (Exception ignored) {
        }

        return memberRepository.findByOrganizationIdAndUserId(orgId, user.getId())
                .map(OrganizationMember::isOrgAdmin)
                .orElse(false);
    }

    public boolean canViewAllOrganizations(User user) {
        if (SecurityUtils.isDevMode()) {
            return true;
        }

        if (!enterpriseSpaceService.hasEnterpriseSpace(user)) {
            return false;
        }
        return enterpriseSpaceMemberRepository.findByEnterpriseSpaceIdAndUserId(
                        enterpriseSpaceService.getCurrentEnterpriseSpace(user).getId(), user.getId())
                .map(m -> m.isSpaceAdmin())
                .orElse(false);
    }

    public List<Long> getManageableOrganizationIds(User user) {
        if (SecurityUtils.isDevMode()) {
            return organizationRepository.findAll().stream()
                    .map(Organization::getId)
                    .collect(Collectors.toList());
        }

        List<Long> accessibleOrgIds = getAccessibleOrganizationIds(user);
        return memberRepository.findAll().stream()
                .filter(m -> accessibleOrgIds.contains(m.getOrganization().getId()))
                .filter(m -> m.getUser().getId().equals(user.getId()) && m.isOrgAdmin())
                .map(m -> m.getOrganization().getId())
                .distinct()
                .collect(Collectors.toList());
    }
}
