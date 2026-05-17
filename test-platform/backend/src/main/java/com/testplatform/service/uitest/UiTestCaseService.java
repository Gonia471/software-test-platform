package com.testplatform.service.uitest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.dto.uitest.CreateOrUpdateCaseRequest;
import com.testplatform.dto.uitest.UiTestCaseDto;
import com.testplatform.entity.Organization;
import com.testplatform.entity.OrganizationMember;
import com.testplatform.entity.uitest.UiTestCase;
import com.testplatform.repository.OrganizationMemberRepository;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.repository.uitest.UiTestCaseRepository;
import com.testplatform.repository.uitest.UiTestExecutionRepository;
import com.testplatform.service.OrganizationPermissionService;
import com.testplatform.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class UiTestCaseService {

    private final UiTestCaseRepository testCaseRepository;
    private final UiTestExecutionRepository executionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationPermissionService permissionService;
    private final UiTestCategoryService testCategoryService;
    private final ObjectMapper objectMapper;

    public UiTestCaseService(
            UiTestCaseRepository testCaseRepository,
            UiTestExecutionRepository executionRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository memberRepository,
            OrganizationPermissionService permissionService,
            UiTestCategoryService testCategoryService,
            ObjectMapper objectMapper) {
        this.testCaseRepository = testCaseRepository;
        this.executionRepository = executionRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.permissionService = permissionService;
        this.testCategoryService = testCategoryService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UiTestCaseDto create(CreateOrUpdateCaseRequest req, com.testplatform.entity.User user) {
        validateRequest(req, true);
        UiTestCase entity = new UiTestCase();
        entity.setName(req.getName() != null && !req.getName().isBlank() ? req.getName() : "未命名用例");
        entity.setDescription(req.getDescription());
        entity.setModuleKey(req.getModuleKey().trim());
        entity.setStepsJson(writeSteps(req.getSteps()));
        entity.setUser(user);

        if (req.getOrganizationId() != null) {
            Organization org = organizationRepository.findById(req.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));
            if (!permissionService.canViewOrganization(org.getId(), user)) {
                throw new IllegalArgumentException("无权限在该组织创建用例");
            }
            entity.setOrganization(org);
            testCategoryService.ensureCategoryExists(org.getId(), req.getModuleKey(), user);
        }

        if (req.getProjectId() != null && entity.getOrganization() != null) {
            entity.setProject(req.getProjectId() > 0 ? entity.getOrganization().getProjects().stream()
                    .filter(p -> p.getId().equals(req.getProjectId()))
                    .findFirst()
                    .orElse(null) : null);
        }

        UiTestCase saved = testCaseRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public UiTestCaseDto update(Long id, CreateOrUpdateCaseRequest req, com.testplatform.entity.User user) {
        validateRequest(req, false);
        UiTestCase entity = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("测试用例不存在: " + id));

        if (!SecurityUtils.isDevMode()) {
            if (entity.getUser() != null && !entity.getUser().getId().equals(user.getId())) {
                if (entity.getOrganization() == null || !permissionService.canManageOrganization(entity.getOrganization().getId(), user)) {
                    throw new ResponseStatusException(FORBIDDEN, "您没有修改此用例的权限");
                }
            }
        }

        if (req.getName() != null) entity.setName(req.getName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getModuleKey() != null) {
            entity.setModuleKey(req.getModuleKey().trim());
            if (entity.getOrganization() != null) {
                testCategoryService.ensureCategoryExists(entity.getOrganization().getId(), entity.getModuleKey(), user);
            }
        }
        if (req.getSteps() != null) entity.setStepsJson(writeSteps(req.getSteps()));
        UiTestCase saved = testCaseRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<UiTestCaseDto> listAll() {
        return listByCurrentUserOrganizations();
    }

    @Transactional(readOnly = true)
    public List<UiTestCaseDto> listByCurrentUserOrganizations() {
        com.testplatform.entity.User user = SecurityUtils.getCurrentUser();

        List<Long> orgIds = permissionService.getAccessibleOrganizationIds(user);

        if (orgIds.isEmpty()) {
            return List.of();
        }

        return testCaseRepository.findAllByOrganizationIdIn(orgIds).stream()
                .map(this::toDtoWithoutSteps)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UiTestCaseDto> listByOrganization(Long orgId, com.testplatform.entity.User user) {
        if (user == null) {
            throw new ResponseStatusException(FORBIDDEN, "当前登录状态无效，请重新登录");
        }
        if (!permissionService.canViewOrganization(orgId, user)) {
            throw new ResponseStatusException(FORBIDDEN, "您没有访问该组织用例的权限");
        }

        List<UiTestCase> cases = testCaseRepository.findAllByOrganizationId(orgId);
        return cases.stream()
                .map(this::toDtoWithoutSteps)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        UiTestCase entity = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("测试用例不存在: " + id));

        if (!SecurityUtils.isDevMode()) {
            com.testplatform.entity.User user = SecurityUtils.getCurrentUser();
            if (entity.getUser() != null && !entity.getUser().getId().equals(user.getId())) {
                if (entity.getOrganization() == null || !permissionService.canManageOrganization(entity.getOrganization().getId(), user)) {
                    throw new ResponseStatusException(FORBIDDEN, "您没有删除此用例的权限");
                }
            }
        }

        // 手动清理执行记录（如果 DB 级级联删除失效或 Hibernate 缓存问题）
        executionRepository.deleteByTestCaseId(id);

        testCaseRepository.deleteById(id);
    }

    private String writeSteps(List<Map<String, Object>> steps) {
        try {
            return objectMapper.writeValueAsString(steps != null ? steps : List.of());
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化步骤失败", e);
        }
    }

    private UiTestCaseDto toDto(UiTestCase entity) {
        UiTestCaseDto dto = new UiTestCaseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setModuleKey(resolveModuleKey(entity));
        dto.setCreator(resolveCreator(entity));
        dto.setSteps(readSteps(entity.getStepsJson()));
        dto.setOrganizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null);
        dto.setProjectId(entity.getProject() != null ? entity.getProject().getId() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private UiTestCaseDto toDtoWithoutSteps(UiTestCase entity) {
        UiTestCaseDto dto = new UiTestCaseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setModuleKey(resolveModuleKey(entity));
        dto.setCreator(resolveCreator(entity));
        dto.setOrganizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null);
        dto.setProjectId(entity.getProject() != null ? entity.getProject().getId() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private void validateRequest(CreateOrUpdateCaseRequest req, boolean creating) {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (creating) {
            if (req.getName() == null || req.getName().isBlank()) {
                throw new IllegalArgumentException("用例名称不能为空");
            }
            if (req.getModuleKey() == null || req.getModuleKey().isBlank()) {
                throw new IllegalArgumentException("用例分类不能为空");
            }
        } else if (req.getModuleKey() != null && req.getModuleKey().isBlank()) {
            throw new IllegalArgumentException("用例分类不能为空");
        }
    }

    @Transactional(readOnly = true)
    public UiTestCaseDto getDetail(Long id) {
        UiTestCase entity = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("测试用例不存在: " + id));

        if (!SecurityUtils.isDevMode() && entity.getOrganization() != null) {
            com.testplatform.entity.User user = SecurityUtils.getCurrentUser();
            if (!permissionService.canViewOrganization(entity.getOrganization().getId(), user)) {
                throw new ResponseStatusException(FORBIDDEN, "您没有访问此用例的权限");
            }
        }

        return toDto(entity);
    }

    private String resolveModuleKey(UiTestCase entity) {
        return entity.getModuleKey() != null && !entity.getModuleKey().isBlank()
                ? entity.getModuleKey()
                : "test";
    }

    private String resolveCreator(UiTestCase entity) {
        return entity.getUser() != null && entity.getUser().getUsername() != null
                ? entity.getUser().getUsername()
                : "";
    }

    private List<Map<String, Object>> readSteps(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("解析用例步骤失败", e);
        }
    }
}
