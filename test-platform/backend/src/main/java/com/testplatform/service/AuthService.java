package com.testplatform.service;

import com.testplatform.dto.AuthResponse;
import com.testplatform.entity.Organization;
import com.testplatform.entity.OrganizationMember;
import com.testplatform.entity.User;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.repository.UserRepository;
import com.testplatform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse registerWithOrg(String phone, String orgName, String description) {
        User user = userRepository.findByPhone(phone).orElseGet(() -> {
            User newUser = new User("user_" + System.currentTimeMillis());
            newUser.setPhone(phone);
            newUser.setIsDevMode(false);
            return userRepository.save(newUser);
        });

        if (organizationRepository.findAllByMember(user).size() > 0) {
            throw new RuntimeException("该用户已是某组织成员");
        }

        Organization org = new Organization();
        org.setName(orgName);
        org.setDescription(description);
        org.setColor("#409EFF");
        org.setOwner(user);
        org = organizationRepository.save(org);

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(org);
        member.setUser(user);
        member.setRole(OrganizationMember.Role.SPACE_CREATOR);
        member.setCanRead(true);
        member.setCanWrite(true);
        org.getMembers().add(member);
        organizationRepository.save(org);

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getPhone(), user.getId(), user.isDevMode());
    }

    public AuthResponse loginWithCode(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("手机号未注册"));

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getPhone(), user.getId(), user.isDevMode());
    }
}