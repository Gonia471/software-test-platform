package com.testplatform.service;

import com.testplatform.dto.EnterpriseSpaceDto;
import com.testplatform.dto.EnterpriseSpaceMemberDto;
import com.testplatform.entity.EnterpriseSpace;
import com.testplatform.entity.EnterpriseSpaceMember;
import com.testplatform.entity.User;
import com.testplatform.repository.EnterpriseSpaceMemberRepository;
import com.testplatform.repository.EnterpriseSpaceRepository;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnterpriseSpaceService {

    private final EnterpriseSpaceRepository enterpriseSpaceRepository;
    private final EnterpriseSpaceMemberRepository enterpriseSpaceMemberRepository;

    @Transactional(readOnly = true)
    public EnterpriseSpace getCurrentEnterpriseSpace(User user) {
        if (SecurityUtils.isDevMode()) {
            return enterpriseSpaceRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前暂无可用企业空间"));
        }
        return enterpriseSpaceRepository.findAllByMember(user).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前用户尚未加入企业空间"));
    }

    @Transactional(readOnly = true)
    public EnterpriseSpaceMember getCurrentMembership(User user) {
        EnterpriseSpace currentSpace = getCurrentEnterpriseSpace(user);
        return enterpriseSpaceMemberRepository.findByEnterpriseSpaceIdAndUserId(currentSpace.getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "您不是当前企业空间成员"));
    }

    @Transactional(readOnly = true)
    public boolean hasEnterpriseSpace(User user) {
        if (SecurityUtils.isDevMode()) {
            return !enterpriseSpaceRepository.findAll().isEmpty();
        }
        return !enterpriseSpaceRepository.findAllByMember(user).isEmpty();
    }

    @Transactional(readOnly = true)
    public EnterpriseSpaceDto getCurrentSpaceDto(User user) {
        EnterpriseSpace space = getCurrentEnterpriseSpace(user);
        return toDto(space);
    }

    @Transactional(readOnly = true)
    public List<EnterpriseSpaceMemberDto> getCurrentSpaceMembers(User user) {
        EnterpriseSpace space = getCurrentEnterpriseSpace(user);
        return enterpriseSpaceMemberRepository.findAllByEnterpriseSpaceId(space.getId()).stream()
                .map(this::toMemberDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public void ensureSpaceAdmin(User user) {
        if (SecurityUtils.isDevMode()) {
            return;
        }
        EnterpriseSpaceMember member = getCurrentMembership(user);
        if (!member.isSpaceAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有企业空间管理员才能执行该操作");
        }
    }

    private EnterpriseSpaceDto toDto(EnterpriseSpace space) {
        return EnterpriseSpaceDto.builder()
                .id(space.getId())
                .name(space.getName())
                .description(space.getDescription())
                .color(space.getColor())
                .ownerId(space.getOwner().getId())
                .ownerUsername(space.getOwner().getUsername())
                .memberCount(space.getMembers() != null ? space.getMembers().size() : 0)
                .organizationCount(space.getOrganizations() != null ? space.getOrganizations().size() : 0)
                .createdAt(space.getCreatedAt())
                .updatedAt(space.getUpdatedAt())
                .build();
    }

    private EnterpriseSpaceMemberDto toMemberDto(EnterpriseSpaceMember member) {
        return EnterpriseSpaceMemberDto.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .phone(member.getUser().getPhone())
                .role(member.getRole())
                .canRead(member.getCanRead())
                .canWrite(member.getCanWrite())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
