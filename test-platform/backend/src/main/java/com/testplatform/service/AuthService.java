package com.testplatform.service;

import com.testplatform.dto.AuthResponse;
import com.testplatform.dto.PendingInvitationDto;
import com.testplatform.entity.EnterpriseSpace;
import com.testplatform.entity.EnterpriseSpaceMember;
import com.testplatform.entity.Invitation;
import com.testplatform.entity.User;
import com.testplatform.repository.EnterpriseSpaceMemberRepository;
import com.testplatform.repository.EnterpriseSpaceRepository;
import com.testplatform.repository.InvitationRepository;
import com.testplatform.repository.UserRepository;
import com.testplatform.util.JwtUtil;
import com.testplatform.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EnterpriseSpaceRepository enterpriseSpaceRepository;
    private final EnterpriseSpaceMemberRepository enterpriseSpaceMemberRepository;
    private final InvitationRepository invitationRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse registerWithOrg(String phone, String orgName, String description) {
        String normalizedPhone = PhoneUtils.normalizeAndValidate(phone);
        User user = userRepository.findByPhone(normalizedPhone).orElseGet(() -> {
            User newUser = new User("user_" + System.currentTimeMillis());
            newUser.setPhone(normalizedPhone);
            newUser.setIsDevMode(false);
            return userRepository.save(newUser);
        });

        if (!enterpriseSpaceRepository.findAllByMember(user).isEmpty()) {
            throw new RuntimeException("该用户已拥有或加入企业空间");
        }

        EnterpriseSpace space = new EnterpriseSpace();
        space.setName(orgName);
        space.setDescription(description);
        space.setColor("#409EFF");
        space.setOwner(user);
        space = enterpriseSpaceRepository.save(space);

        EnterpriseSpaceMember member = new EnterpriseSpaceMember();
        member.setEnterpriseSpace(space);
        member.setUser(user);
        member.setRole(EnterpriseSpaceMember.Role.SPACE_CREATOR);
        member.setCanRead(true);
        member.setCanWrite(true);
        enterpriseSpaceMemberRepository.save(member);

        String token = jwtUtil.generateToken(user.getUsername());
        return buildAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse loginWithCode(String phone) {
        String normalizedPhone = PhoneUtils.normalizeAndValidate(phone);
        User user = userRepository.findByPhone(normalizedPhone).orElseGet(() -> {
            User newUser = new User("user_" + System.currentTimeMillis());
            newUser.setPhone(normalizedPhone);
            newUser.setIsDevMode(false);
            return userRepository.save(newUser);
        });

        String token = jwtUtil.generateToken(user.getUsername());
        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        List<EnterpriseSpace> spaces = enterpriseSpaceRepository.findAllByMember(user);
        List<PendingInvitationDto> pendingInvitations = invitationRepository.findPendingInvitationsByPhone(user.getPhone()).stream()
                .map(this::toPendingInvitation)
                .collect(Collectors.toList());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setPhone(user.getPhone());
        response.setUserId(user.getId());
        response.setIsDevMode(user.isDevMode());
        response.setHasEnterpriseSpace(!spaces.isEmpty());
        if (!spaces.isEmpty()) {
            response.setEnterpriseSpaceId(spaces.get(0).getId());
            response.setEnterpriseSpaceName(spaces.get(0).getName());
        }
        response.setPendingInvitations(pendingInvitations);
        return response;
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
}
