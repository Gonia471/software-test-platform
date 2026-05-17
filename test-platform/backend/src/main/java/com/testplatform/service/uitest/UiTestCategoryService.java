package com.testplatform.service.uitest;

import com.testplatform.dto.uitest.CreateUiTestCategoryRequest;
import com.testplatform.dto.uitest.UiTestCategoryDto;
import com.testplatform.dto.uitest.UpdateUiTestCategoryRequest;
import com.testplatform.entity.Organization;
import com.testplatform.entity.User;
import com.testplatform.entity.uitest.UiTestCategory;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.repository.uitest.UiTestCaseRepository;
import com.testplatform.repository.uitest.UiTestCategoryRepository;
import com.testplatform.service.OrganizationPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class UiTestCategoryService {

    private final UiTestCategoryRepository categoryRepository;
    private final UiTestCaseRepository testCaseRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationPermissionService permissionService;

    public UiTestCategoryService(
            UiTestCategoryRepository categoryRepository,
            UiTestCaseRepository testCaseRepository,
            OrganizationRepository organizationRepository,
            OrganizationPermissionService permissionService) {
        this.categoryRepository = categoryRepository;
        this.testCaseRepository = testCaseRepository;
        this.organizationRepository = organizationRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public List<UiTestCategoryDto> listByOrganization(Long orgId, User user) {
        ensureOrganizationAccessible(orgId, user);
        syncCategoriesFromCases(orgId, user);
        List<UiTestCategoryDto> categories = new ArrayList<>();
        for (UiTestCategory category : categoryRepository.findAllByOrganizationId(orgId)) {
            categories.add(toDto(category));
        }
        categories.sort(Comparator.comparing(item -> item.getName() == null ? "" : item.getName(), String.CASE_INSENSITIVE_ORDER));
        return categories;
    }

    @Transactional
    public UiTestCategoryDto create(CreateUiTestCategoryRequest req, User user) {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (req.getOrganizationId() == null) {
            throw new IllegalArgumentException("所属组织不能为空");
        }

        ensureOrganizationAccessible(req.getOrganizationId(), user);

        String moduleKey = normalizeModuleKey(req.getKey() != null ? req.getKey() : req.getName());
        String displayName = normalizeModuleKey(req.getName() != null ? req.getName() : req.getKey());
        if (moduleKey.isEmpty() || displayName.isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }

        UiTestCategory category = ensureCategoryExists(req.getOrganizationId(), moduleKey, displayName, user);
        return toDto(category);
    }

    @Transactional
    public UiTestCategoryDto update(Long categoryId, UpdateUiTestCategoryRequest req, User user) {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        UiTestCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        ensureOrganizationManageable(category.getOrganization().getId(), user);

        String displayName = normalizeModuleKey(req.getName());
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }

        category.setDisplayName(displayName);
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long categoryId, User user) {
        UiTestCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        Long orgId = category.getOrganization().getId();
        ensureOrganizationManageable(orgId, user);

        long caseCount = testCaseRepository.countByOrganizationIdAndModuleKey(orgId, category.getModuleKey());
        if (caseCount > 0) {
            throw new IllegalArgumentException("该分类下仍有关联用例，无法删除");
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public UiTestCategory ensureCategoryExists(Long orgId, String moduleKey, User user) {
        String normalized = normalizeModuleKey(moduleKey);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("用例分类不能为空");
        }
        return ensureCategoryExists(orgId, normalized, normalized, user);
    }

    @Transactional
    public UiTestCategory ensureCategoryExists(Long orgId, String moduleKey, String displayName, User user) {
        String normalizedKey = normalizeModuleKey(moduleKey);
        String normalizedName = normalizeModuleKey(displayName);
        if (normalizedKey.isEmpty() || normalizedName.isEmpty()) {
            throw new IllegalArgumentException("用例分类不能为空");
        }

        return categoryRepository.findByOrganizationIdAndModuleKey(orgId, normalizedKey)
                .orElseGet(() -> {
                    Organization organization = organizationRepository.findById(orgId)
                            .orElseThrow(() -> new IllegalArgumentException("组织不存在"));
                    UiTestCategory category = new UiTestCategory();
                    category.setOrganization(organization);
                    category.setModuleKey(normalizedKey);
                    category.setDisplayName(normalizedName);
                    category.setCreatedBy(user);
                    return categoryRepository.save(category);
                });
    }

    @Transactional
    public void syncCategoriesFromCases(Long orgId, User user) {
        for (String moduleKey : testCaseRepository.findDistinctModuleKeysByOrganizationId(orgId)) {
            String normalized = normalizeModuleKey(moduleKey);
            if (!normalized.isEmpty()) {
                ensureCategoryExists(orgId, normalized, normalized, user);
            }
        }
    }

    private void ensureOrganizationAccessible(Long orgId, User user) {
        if (user == null) {
            throw new ResponseStatusException(FORBIDDEN, "当前登录状态无效，请重新登录");
        }
        if (!permissionService.canViewOrganization(orgId, user)) {
            throw new ResponseStatusException(FORBIDDEN, "您没有访问该组织分类的权限");
        }
    }

    private void ensureOrganizationManageable(Long orgId, User user) {
        if (user == null) {
            throw new ResponseStatusException(FORBIDDEN, "当前登录状态无效，请重新登录");
        }
        if (!permissionService.canManageOrganization(orgId, user)) {
            throw new ResponseStatusException(FORBIDDEN, "您没有管理该组织分类的权限");
        }
    }

    private UiTestCategoryDto toDto(UiTestCategory category) {
        UiTestCategoryDto dto = new UiTestCategoryDto();
        dto.setId(category.getId());
        dto.setKey(category.getModuleKey());
        dto.setName(category.getDisplayName());
        dto.setOrganizationId(category.getOrganization() != null ? category.getOrganization().getId() : null);
        long caseCount = category.getOrganization() != null
                ? testCaseRepository.countByOrganizationIdAndModuleKey(category.getOrganization().getId(), category.getModuleKey())
                : 0;
        dto.setCaseCount(caseCount);
        dto.setDeletable(caseCount == 0);
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }

    private String normalizeModuleKey(String moduleKey) {
        return moduleKey == null ? "" : moduleKey.trim();
    }
}
