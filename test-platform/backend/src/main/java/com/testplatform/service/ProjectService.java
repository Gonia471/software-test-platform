package com.testplatform.service;

import com.testplatform.dto.*;
import com.testplatform.entity.Organization;
import com.testplatform.entity.OrganizationMember;
import com.testplatform.entity.Project;
import com.testplatform.entity.User;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.repository.ProjectRepository;
import com.testplatform.repository.UserRepository;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ProjectExecutionService projectExecutionService;

    @Transactional(readOnly = true)
    public List<ProjectDto> getUserProjects(User user) {
        if (SecurityUtils.isDevMode()) {
            return projectRepository.findAll().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return organizationRepository.findAllByMember(user).stream()
                .flatMap(org -> projectRepository.findAllByOrganizationId(org.getId()).stream())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request, User user) {
        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "组织不存在"));

        if (!SecurityUtils.isDevMode() && !organizationRepository.isMember(org.getId(), user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限在该组织创建项目");
        }

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setColor(request.getColor() != null ? request.getColor() : "#409EFF");
        project.setOrganization(org);
        project.setOwner(user);
        project.setType(normalizeType(request.getType()));
        project.setItemsJson(request.getItemsJson());
        project.setCronExpression(request.getCronExpression());
        project.setLoopCount(request.getLoopCount() != null ? request.getLoopCount() : 1);
        project.setUiInstanceId(request.getUiInstanceId());
        project.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        Project saved = projectRepository.save(project);
        projectExecutionService.scheduleProject(saved);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ProjectDto getProject(Long id, User user) {
        Project project = findProjectOrThrow(id);

        if (!SecurityUtils.isDevMode() && !organizationRepository.isMember(project.getOrganization().getId(), user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限访问该项目");
        }

        return toDto(project);
    }

    @Transactional
    public ProjectDto updateProject(Long id, UpdateProjectRequest request, User user) {
        Project project = findProjectOrThrow(id);

        if (!SecurityUtils.isDevMode() && !project.getOwner().getId().equals(user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "只有项目所有者才能修改项目信息");
        }

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getColor() != null) {
            project.setColor(request.getColor());
        }
        if (request.getType() != null) {
            project.setType(normalizeType(request.getType()));
        }
        if (request.getItemsJson() != null) {
            project.setItemsJson(request.getItemsJson());
        }
        if (request.getCronExpression() != null) {
            project.setCronExpression(request.getCronExpression());
        }
        if (request.getLoopCount() != null) {
            project.setLoopCount(request.getLoopCount());
        }
        if (request.getUiInstanceId() != null || "API".equalsIgnoreCase(project.getType())) {
            project.setUiInstanceId(request.getUiInstanceId());
        }
        if (request.getEnabled() != null) {
            project.setEnabled(request.getEnabled());
        }

        Project saved = projectRepository.save(project);
        projectExecutionService.cancelSchedule(saved.getId());
        projectExecutionService.scheduleProject(saved);
        return toDto(saved);
    }

    @Transactional
    public void deleteProject(Long id, User user) {
        Project project = findProjectOrThrow(id);

        if (!SecurityUtils.isDevMode() && !project.getOwner().getId().equals(user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "只有项目所有者才能删除项目");
        }

        projectExecutionService.cancelSchedule(project.getId());
        projectRepository.delete(project);
    }

    private Project findProjectOrThrow(Long id) {
        return projectRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "项目不存在"));
    }

    private ProjectDto toDto(Project project) {
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

    private String normalizeType(String type) {
        String normalized = type == null ? "API" : type.trim().toUpperCase();
        if (!"API".equals(normalized) && !"UI".equals(normalized)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "项目类型仅支持 API 或 UI");
        }
        return normalized;
    }
}
