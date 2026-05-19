package com.testplatform.service;

import com.testplatform.dto.*;
import com.testplatform.entity.EnterpriseSpace;
import com.testplatform.entity.EnterpriseSpaceMember;
import com.testplatform.entity.Organization;
import com.testplatform.entity.OrganizationMember;
import com.testplatform.entity.Project;
import com.testplatform.entity.User;
import com.testplatform.repository.EnterpriseSpaceMemberRepository;
import com.testplatform.repository.OrganizationMemberRepository;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.repository.ProjectRepository;
import com.testplatform.repository.UserRepository;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final EnterpriseSpaceService enterpriseSpaceService;
    private final EnterpriseSpaceMemberRepository enterpriseSpaceMemberRepository;

    @Transactional(readOnly = true)
    public List<OrganizationDto> getUserOrganizations(User user) {
        List<Organization> orgs;
        if (SecurityUtils.isDevMode()) {
            log.info("开发模式：返回所有组织");
            orgs = organizationRepository.findAll();
        } else {
            orgs = organizationRepository.findAllByMember(user);
        }
        return orgs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrganizationDto createOrganization(CreateOrganizationRequest request, User user) {
        log.info("创建组织: name={}, user={}", request.getName(), user.getUsername());
        EnterpriseSpace enterpriseSpace = enterpriseSpaceService.getCurrentEnterpriseSpace(user);
        Organization org = new Organization();
        org.setName(request.getName());
        org.setDescription(request.getDescription());
        org.setColor(request.getColor() != null ? request.getColor() : "#409EFF");
        org.setEnterpriseSpace(enterpriseSpace);
        org.setOwner(user);

        org = organizationRepository.save(org);

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(org);
        member.setUser(user);
        member.setRole(OrganizationMember.Role.ORG_ADMIN);
        member.setCanRead(true);
        member.setCanWrite(true);
        memberRepository.save(member);

        return toDto(org);
    }

    public OrganizationDto getOrganization(Long id, User user) {
        Organization org = findOrgOrThrow(id);

        if (!SecurityUtils.isDevMode() && !organizationRepository.isMember(id, user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限访问该组织");
        }

        return toDto(org);
    }

    @Transactional
    public OrganizationDto updateOrganization(Long id, UpdateOrganizationRequest request, User user) {
        Organization org = findOrgOrThrow(id);
        if (!canManageOrganization(org, user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "只有企业空间管理员或组织管理员才能修改组织信息");
        }

        if (request.getName() != null) {
            org.setName(request.getName());
        }
        if (request.getDescription() != null) {
            org.setDescription(request.getDescription());
        }
        if (request.getColor() != null) {
            org.setColor(request.getColor());
        }

        return toDto(organizationRepository.save(org));
    }

    @Transactional
    public void deleteOrganization(Long id, User user) {
        Organization org = findOrgOrThrow(id);
        if (SecurityUtils.isDevMode()) {
            organizationRepository.delete(org);
            return;
        }
        EnterpriseSpaceMember spaceMember = enterpriseSpaceMemberRepository
                .findByEnterpriseSpaceIdAndUserId(org.getEnterpriseSpace().getId(), user.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "您不是当前企业空间成员"));
        if (!spaceMember.isSpaceAdmin()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "只有企业空间管理员才能删除组织");
        }

        organizationRepository.delete(org);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getOrganizationProjects(Long orgId, User user) {
        if (!SecurityUtils.isDevMode() && !organizationRepository.isMember(orgId, user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限访问该组织");
        }

        return projectRepository.findProjectListByOrganizationId(orgId).stream()
                .map(this::toProjectDto)
                .collect(Collectors.toList());
    }

    private Organization findOrgOrThrow(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "组织不存在"));
    }

    private OrganizationDto toDto(Organization org) {
        OrganizationDto dto = OrganizationDto.builder()
                .id(org.getId())
                .name(org.getName())
                .description(org.getDescription())
                .color(org.getColor())
                .ownerId(org.getOwner().getId())
                .ownerUsername(org.getOwner().getUsername())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
        dto.setMemberCount(memberRepository.countByOrganizationId(org.getId()));
        dto.setProjectCount(projectRepository.countByOrganizationId(org.getId()));
        return dto;
    }

    private ProjectDto toProjectDto(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .color(project.getColor())
                .organizationId(project.getOrganization().getId())
                .organizationName(project.getOrganization().getName())
                .ownerId(project.getOwner().getId())
                .ownerUsername(project.getOwner().getUsername())
                .type(project.getType())
                .itemsJson(project.getItemsJson())
                .cronExpression(project.getCronExpression())
                .loopCount(project.getLoopCount())
                .uiInstanceId(project.getUiInstanceId())
                .enabled(project.getEnabled())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ProjectDto toProjectDto(ProjectRepository.ProjectListProjection project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .color(project.getColor())
                .organizationId(project.getOrganizationId())
                .organizationName(project.getOrganizationName())
                .ownerId(project.getOwnerId())
                .ownerUsername(project.getOwnerUsername())
                .type(project.getType())
                .itemsJson(project.getItemsJson())
                .cronExpression(project.getCronExpression())
                .loopCount(project.getLoopCount())
                .uiInstanceId(project.getUiInstanceId())
                .enabled(project.getEnabled())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberDto> getOrganizationMembers(Long orgId, User user) {
        if (!SecurityUtils.isDevMode() && !organizationRepository.isMember(orgId, user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限访问该组织");
        }

        Organization org = organizationRepository.findByIdWithMembers(orgId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "组织不存在"));
        return org.getMembers().stream()
                .map(this::toMemberDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrganizationMemberDto updateMemberRole(Long orgId, Long memberId, UpdateMemberRequest request, User user) {
        Organization org = organizationRepository.findByIdWithMembers(orgId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "组织不存在"));
        if (!canManageOrganization(org, user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "只有企业空间管理员或组织管理员才能修改成员角色");
        }

        OrganizationMember targetMember = org.getMembers().stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "成员不存在"));

        if (!SecurityUtils.isDevMode() && targetMember.getUser().getId().equals(org.getOwner().getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无法修改组织负责人的角色");
        }

        if (request.getRole() != null) {
            targetMember.setRole(OrganizationMember.Role.valueOf(request.getRole()));
        }
        if (request.getCanRead() != null) {
            targetMember.setCanRead(request.getCanRead());
        }
        if (request.getCanWrite() != null) {
            targetMember.setCanWrite(request.getCanWrite());
        }

        organizationRepository.save(org);
        return toMemberDto(targetMember);
    }

    @Transactional
    public void removeMember(Long orgId, Long memberId, User user) {
        Organization org = organizationRepository.findByIdWithMembers(orgId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "组织不存在"));
        if (!canManageOrganization(org, user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "只有企业空间管理员或组织管理员才能移除成员");
        }

        OrganizationMember targetMember = org.getMembers().stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "成员不存在"));

        if (!SecurityUtils.isDevMode() && targetMember.getUser().getId().equals(org.getOwner().getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无法移除组织负责人");
        }

        org.getMembers().remove(targetMember);
        organizationRepository.save(org);
    }

    public OrganizationMemberDto getMemberInfo(Long orgId, User user) {
        if (SecurityUtils.isDevMode()) {
            organizationRepository.findById(orgId)
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            HttpStatus.NOT_FOUND, "组织不存在"));
            return OrganizationMemberDto.builder()
                    .id(null)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .phone(user.getPhone())
                    .role(OrganizationMember.Role.ORG_ADMIN)
                    .canRead(true)
                    .canWrite(true)
                    .joinedAt(null)
                    .build();
        }
        OrganizationMember member = organizationRepository.findMember(orgId, user)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "您不是该组织成员"));

        return toMemberDto(member);
    }

    @Transactional(readOnly = true)
    public List<EnterpriseSpaceMemberDto> getAvailableEnterpriseMembers(Long orgId, User user) {
        Organization org = organizationRepository.findByIdWithMembers(orgId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "组织不存在"));
        if (!canManageOrganization(org, user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "只有企业空间管理员或组织管理员才能添加成员");
        }

        List<Long> existingUserIds = org.getMembers().stream()
                .map(m -> m.getUser().getId())
                .toList();

        return enterpriseSpaceMemberRepository.findAllByEnterpriseSpaceId(org.getEnterpriseSpace().getId()).stream()
                .filter(member -> !existingUserIds.contains(member.getUser().getId()))
                .map(this::toEnterpriseMemberDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrganizationMemberDto addMember(Long orgId, AddOrganizationMemberRequest request, User user) {
        Organization org = organizationRepository.findByIdWithMembers(orgId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "组织不存在"));
        if (!canManageOrganization(org, user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "只有企业空间管理员或组织管理员才能添加成员");
        }

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "用户不存在"));

        if (!enterpriseSpaceMemberRepository.existsByEnterpriseSpaceIdAndUserId(
                org.getEnterpriseSpace().getId(), targetUser.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "该用户尚未加入当前企业空间");
        }

        if (memberRepository.findByOrganizationIdAndUserId(orgId, targetUser.getId()).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "该成员已在当前组织中");
        }

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(org);
        member.setUser(targetUser);
        member.setRole(parseOrgRole(request.getRole()));
        member.setCanRead(request.getCanRead() != null ? request.getCanRead() : true);
        member.setCanWrite(request.getCanWrite() != null ? request.getCanWrite() : false);
        return toMemberDto(memberRepository.save(member));
    }

    private boolean canManageOrganization(Organization org, User user) {
        if (SecurityUtils.isDevMode()) {
            return true;
        }
        boolean isSpaceAdmin = enterpriseSpaceMemberRepository
                .findByEnterpriseSpaceIdAndUserId(org.getEnterpriseSpace().getId(), user.getId())
                .map(EnterpriseSpaceMember::isSpaceAdmin)
                .orElse(false);
        if (isSpaceAdmin) {
            return true;
        }
        return organizationRepository.findMember(org.getId(), user)
                .map(OrganizationMember::isOrgAdmin)
                .orElse(false);
    }

    private OrganizationMember.Role parseOrgRole(String role) {
        if (role == null || role.isBlank()) {
            return OrganizationMember.Role.MEMBER;
        }
        OrganizationMember.Role parsed = OrganizationMember.Role.valueOf(role);
        if (parsed == OrganizationMember.Role.SPACE_ADMIN || parsed == OrganizationMember.Role.SPACE_CREATOR) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "组织成员角色仅支持 ORG_ADMIN 或 MEMBER");
        }
        return parsed;
    }

    private OrganizationMemberDto toMemberDto(OrganizationMember member) {
        return OrganizationMemberDto.builder()
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

    private EnterpriseSpaceMemberDto toEnterpriseMemberDto(EnterpriseSpaceMember member) {
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
