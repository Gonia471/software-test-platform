package com.testplatform.service;

import com.testplatform.dto.CreateInvitationRequest;
import com.testplatform.dto.InvitationCheckResponse;
import com.testplatform.dto.InvitationDto;
import com.testplatform.entity.Invitation;
import com.testplatform.entity.Organization;
import com.testplatform.entity.OrganizationMember;
import com.testplatform.entity.User;
import com.testplatform.repository.InvitationRepository;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public InvitationDto createInvitation(Long orgId, CreateInvitationRequest request, User inviter) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

        OrganizationMember member = organizationRepository.findMember(orgId, inviter)
                .orElseThrow(() -> new IllegalArgumentException("您不是该组织成员"));

        if (!member.isOrgAdmin()) {
            throw new IllegalArgumentException("只有空间管理员或组织管理员才能生成邀请码");
        }

        String code = generateUniqueCode();

        Invitation invitation = new Invitation();
        invitation.setOrganization(org);
        invitation.setInvitedBy(inviter);
        invitation.setInvitationCode(code);
        invitation.setInvitedPhone(request.getInvitedPhone());
        invitation.setEffectiveAt(Instant.now());
        invitation.setExpiredAt(Instant.now().plus(
                (request.getValidDays() != null ? request.getValidDays() : 7), ChronoUnit.DAYS));

        invitation = invitationRepository.save(invitation);
        return toDto(invitation);
    }

    public List<InvitationDto> getOrganizationInvitations(Long orgId, User user) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

        OrganizationMember member = organizationRepository.findMember(orgId, user)
                .orElseThrow(() -> new IllegalArgumentException("您不是该组织成员"));

        if (!member.isOrgAdmin()) {
            throw new IllegalArgumentException("只有空间管理员或组织管理员才能查看邀请记录");
        }

        return invitationRepository.findByOrganization(org).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public InvitationCheckResponse checkInvitationByPhone(String phone) {
        List<Invitation> invitations = invitationRepository.findPendingInvitationsByPhone(phone);

        if (invitations.isEmpty()) {
            return new InvitationCheckResponse(false, null, null, null, null, null);
        }

        Invitation invitation = invitations.get(0);
        return new InvitationCheckResponse(
                true,
                invitation.getOrganization().getId(),
                invitation.getOrganization().getName(),
                invitation.getInvitedPhone(),
                invitation.getInvitedBy().getUsername(),
                invitation.getCreatedAt()
        );
    }

    @Transactional
    public void useInvitation(String code, User user) {
        Invitation invitation = invitationRepository.findByInvitationCode(code)
                .orElseThrow(() -> new IllegalArgumentException("邀请码无效"));

        if (!invitation.isValid()) {
            throw new IllegalArgumentException("邀请码已失效或已使用");
        }

        invitation.setUsed(true);
        invitation.setUsedBy(user);
        invitationRepository.save(invitation);
    }

    public Invitation getValidInvitation(String code) {
        Invitation invitation = invitationRepository.findByInvitationCode(code)
                .orElseThrow(() -> new IllegalArgumentException("邀请码无效"));

        if (!invitation.isValid()) {
            throw new IllegalArgumentException("邀请码已失效或已使用");
        }

        return invitation;
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

    private InvitationDto toDto(Invitation invitation) {
        InvitationDto dto = new InvitationDto();
        dto.setId(invitation.getId());
        dto.setOrganizationId(invitation.getOrganization().getId());
        dto.setOrganizationName(invitation.getOrganization().getName());
        dto.setInvitationCode(invitation.getInvitationCode());
        dto.setInvitedPhone(invitation.getInvitedPhone());
        dto.setUsed(invitation.getUsed());
        if (invitation.getUsedBy() != null) {
            dto.setUsedByUsername(invitation.getUsedBy().getUsername());
        }
        dto.setCreatedAt(invitation.getCreatedAt());
        dto.setExpiredAt(invitation.getExpiredAt());
        return dto;
    }
}
