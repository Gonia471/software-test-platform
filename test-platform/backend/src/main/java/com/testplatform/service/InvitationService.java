package com.testplatform.service;

import com.testplatform.dto.CreateInvitationRequest;
import com.testplatform.dto.InvitationCheckResponse;
import com.testplatform.dto.InvitationDto;
import com.testplatform.dto.PendingInvitationDto;
import com.testplatform.entity.EnterpriseSpace;
import com.testplatform.entity.EnterpriseSpaceMember;
import com.testplatform.entity.Invitation;
import com.testplatform.entity.Organization;
import com.testplatform.entity.OrganizationMember;
import com.testplatform.entity.User;
import com.testplatform.repository.EnterpriseSpaceMemberRepository;
import com.testplatform.repository.InvitationRepository;
import com.testplatform.repository.OrganizationMemberRepository;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.util.PhoneUtils;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final EnterpriseSpaceService enterpriseSpaceService;
    private final EnterpriseSpaceMemberRepository enterpriseSpaceMemberRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public InvitationDto createInvitation(CreateInvitationRequest request, User inviter) {
        enterpriseSpaceService.ensureSpaceAdmin(inviter);
        EnterpriseSpace enterpriseSpace = enterpriseSpaceService.getCurrentEnterpriseSpace(inviter);
        String normalizedPhone = PhoneUtils.normalizeAndValidate(request.getInvitedPhone());

        Organization targetOrganization = null;
        if (request.getOrganizationId() != null) {
            targetOrganization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "组织不存在"));
            if (SecurityUtils.isDevMode()) {
                enterpriseSpace = targetOrganization.getEnterpriseSpace();
            } else if (!targetOrganization.getEnterpriseSpace().getId().equals(enterpriseSpace.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只能邀请加入当前企业空间下的组织");
            }
        }

        Invitation invitation = new Invitation();
        invitation.setEnterpriseSpace(enterpriseSpace);
        invitation.setOrganization(targetOrganization);
        invitation.setInvitedBy(inviter);
        invitation.setInvitationCode(generateUniqueCode());
        invitation.setInvitedPhone(normalizedPhone);
        invitation.setEffectiveAt(Instant.now());
        invitation.setExpiredAt(Instant.now().plus(
                request.getValidDays() != null ? request.getValidDays() : 7,
                ChronoUnit.DAYS));

        return toDto(invitationRepository.save(invitation));
    }

    @Transactional(readOnly = true)
    public List<InvitationDto> getCurrentSpaceInvitations(User user) {
        enterpriseSpaceService.ensureSpaceAdmin(user);
        if (SecurityUtils.isDevMode()) {
            return invitationRepository.findAll().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        EnterpriseSpace enterpriseSpace = enterpriseSpaceService.getCurrentEnterpriseSpace(user);
        return invitationRepository.findByEnterpriseSpace(enterpriseSpace).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvitationCheckResponse checkInvitationByPhone(String phone) {
        String normalizedPhone = PhoneUtils.normalizeAndValidate(phone);
        List<PendingInvitationDto> invitations = invitationRepository.findPendingInvitationsByPhone(normalizedPhone).stream()
                .filter(Invitation::isValid)
                .map(this::toPendingInvitation)
                .collect(Collectors.toList());

        return new InvitationCheckResponse(
                !invitations.isEmpty(),
                invitations,
                normalizedPhone,
                invitations.isEmpty() ? null : invitations.get(0).getInvitedAt()
        );
    }

    @Transactional
    public void acceptInvitation(Long invitationId, User user) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀请不存在"));

        if (!invitation.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邀请已失效或已使用");
        }

        List<EnterpriseSpace> joinedSpaces = enterpriseSpaceService.hasEnterpriseSpace(user)
                ? List.of(enterpriseSpaceService.getCurrentEnterpriseSpace(user))
                : List.of();

        if (!joinedSpaces.isEmpty()
                && !joinedSpaces.get(0).getId().equals(invitation.getEnterpriseSpace().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前版本暂不支持加入多个企业空间");
        }

        if (!enterpriseSpaceMemberRepository.existsByEnterpriseSpaceIdAndUserId(
                invitation.getEnterpriseSpace().getId(), user.getId())) {
            EnterpriseSpaceMember member = new EnterpriseSpaceMember();
            member.setEnterpriseSpace(invitation.getEnterpriseSpace());
            member.setUser(user);
            member.setRole(EnterpriseSpaceMember.Role.MEMBER);
            member.setCanRead(true);
            member.setCanWrite(true);
            enterpriseSpaceMemberRepository.save(member);
        }

        if (invitation.getOrganization() != null
                && organizationMemberRepository.findByOrganizationIdAndUserId(
                invitation.getOrganization().getId(), user.getId()).isEmpty()) {
            OrganizationMember orgMember = new OrganizationMember();
            orgMember.setOrganization(invitation.getOrganization());
            orgMember.setUser(user);
            orgMember.setRole(OrganizationMember.Role.MEMBER);
            orgMember.setCanRead(true);
            orgMember.setCanWrite(true);
            organizationMemberRepository.save(orgMember);
        }

        invitation.setUsed(true);
        invitation.setUsedBy(user);
        invitationRepository.save(invitation);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = generateCode(8);
        } while (invitationRepository.existsByInvitationCode(code));
        return code;
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private PendingInvitationDto toPendingInvitation(Invitation invitation) {
        return new PendingInvitationDto(
                invitation.getId(),
                invitation.getEnterpriseSpace().getId(),
                invitation.getEnterpriseSpace().getName(),
                invitation.getOrganization() != null ? invitation.getOrganization().getId() : null,
                invitation.getOrganization() != null ? invitation.getOrganization().getName() : null,
                invitation.getInvitedPhone(),
                invitation.getInvitedBy().getUsername(),
                invitation.getCreatedAt()
        );
    }

    private InvitationDto toDto(Invitation invitation) {
        InvitationDto dto = new InvitationDto();
        dto.setId(invitation.getId());
        dto.setEnterpriseSpaceId(invitation.getEnterpriseSpace().getId());
        dto.setEnterpriseSpaceName(invitation.getEnterpriseSpace().getName());
        dto.setOrganizationId(invitation.getOrganization() != null ? invitation.getOrganization().getId() : null);
        dto.setOrganizationName(invitation.getOrganization() != null ? invitation.getOrganization().getName() : null);
        dto.setInvitedPhone(invitation.getInvitedPhone());
        dto.setUsed(invitation.getUsed());
        dto.setInvitedByUsername(invitation.getInvitedBy().getUsername());
        if (invitation.getUsedBy() != null) {
            dto.setUsedByUsername(invitation.getUsedBy().getUsername());
        }
        dto.setCreatedAt(invitation.getCreatedAt());
        dto.setExpiredAt(invitation.getExpiredAt());
        return dto;
    }
}
