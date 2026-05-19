package com.testplatform.service.apitest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.dto.apitest.ApiCollectionDto;
import com.testplatform.entity.Organization;
import com.testplatform.entity.User;
import com.testplatform.entity.apitest.ApiCollection;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.repository.apitest.ApiCollectionRepository;
import com.testplatform.service.OrganizationPermissionService;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@RequiredArgsConstructor
public class ApiCollectionService {

    private final ApiCollectionRepository collectionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationPermissionService permissionService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ApiCollectionDto> getCollections(Long userId) {
        User user = SecurityUtils.getCurrentUser();

        List<Long> orgIds = permissionService.getAccessibleOrganizationIds(user);

        if (orgIds.isEmpty()) {
            return List.of();
        }

        List<ApiCollection> roots = collectionRepository.findRootNodesByOrganizationIdIn(orgIds);
        return roots.stream()
                .map(this::toDtoWithChildren)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiCollectionDto create(ApiCollectionDto dto, User user) {
        ApiCollection collection = toEntity(dto);
        collection.setUser(user);

        if (dto.getOrganizationId() != null) {
            Organization org = organizationRepository.findById(dto.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));
            if (!permissionService.canViewOrganization(org.getId(), user)) {
                throw new ResponseStatusException(FORBIDDEN, "您没有在该组织创建接口的权限");
            }
            collection.setOrganization(org);
        }

        if (dto.getParentId() != null) {
            ApiCollection parent = collectionRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("父节点不存在: " + dto.getParentId()));
            
            if (parent.getNodeType() != ApiCollection.NodeType.FOLDER) {
                throw new IllegalArgumentException("接口只能创建在文件夹之下");
            }

            if (!SecurityUtils.isDevMode() && parent.getOrganization() != null) {
                if (!permissionService.canViewOrganization(parent.getOrganization().getId(), user)) {
                    throw new ResponseStatusException(FORBIDDEN, "您没有访问该父节点的权限");
                }
            } else if (!SecurityUtils.isDevMode() && !parent.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(FORBIDDEN, "您没有访问该父节点的权限");
            }
            collection.setParent(parent);
        }

        ApiCollection saved = collectionRepository.save(collection);
        return toDto(saved);
    }

    @Transactional
    public ApiCollectionDto update(Long id, ApiCollectionDto dto, User user) {
        ApiCollection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("集合不存在: " + id));

        if (SecurityUtils.isDevMode()) {
            if (collection.getNodeType() == ApiCollection.NodeType.CASE
                    && collection.getUser() != null
                    && !collection.getUser().isDevMode()) {
                throw new ResponseStatusException(FORBIDDEN, "开发者模式不能修改非开发者创建的测试用例");
            }
        } else {
            if (collection.getUser() != null && !collection.getUser().getId().equals(user.getId())) {
                if (collection.getOrganization() == null
                        || !permissionService.canManageOrganization(collection.getOrganization().getId(), user)) {
                    throw new ResponseStatusException(FORBIDDEN, "您没有修改此 API 用例的权限");
                }
            }
        }

        if (dto.getName() != null) {
            if (dto.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("名称不能为空");
            }
            collection.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            collection.setDescription(dto.getDescription());
        }
        if (dto.getMethod() != null) {
            collection.setMethod(dto.getMethod());
        }
        if (dto.getUrl() != null) {
            collection.setUrl(dto.getUrl());
        }
        if (dto.getParams() != null) {
            collection.setParamsJson(toJson(dto.getParams()));
        }
        if (dto.getHeaders() != null) {
            collection.setHeadersJson(toJson(dto.getHeaders()));
        }
        if (dto.getBodyType() != null) {
            collection.setBodyType(dto.getBodyType());
        }
        if (dto.getBodyRaw() != null) {
            collection.setBodyRaw(dto.getBodyRaw());
        }
        if (dto.getBodyRawType() != null) {
            collection.setBodyRawType(dto.getBodyRawType());
        }
        if (dto.getBodyForm() != null) {
            collection.setBodyForm(toJson(dto.getBodyForm()));
        }
        if (dto.getAuthType() != null) {
            collection.setAuthType(dto.getAuthType());
        }
        if (dto.getAuthConfig() != null) {
            collection.setAuthConfig(toJson(dto.getAuthConfig()));
        }
        if (dto.getAssertions() != null) {
            collection.setAssertions(toJson(dto.getAssertions()));
        }

        ApiCollection.NodeType nodeType = dto.getNodeType();
        if (nodeType == null && dto.getType() != null) {
            try {
                nodeType = ApiCollection.NodeType.valueOf(dto.getType().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        if (nodeType != null) {
            collection.setNodeType(nodeType);
        }

        ApiCollection saved = collectionRepository.save(collection);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id, User user) {
        ApiCollection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("集合不存在"));

        if (!SecurityUtils.isDevMode()) {
            if (collection.getUser() != null && !collection.getUser().getId().equals(user.getId())) {
                if (collection.getOrganization() == null
                        || !permissionService.canManageOrganization(collection.getOrganization().getId(), user)) {
                    throw new ResponseStatusException(FORBIDDEN, "您没有删除此 API 用例的权限");
                }
            }
        }

        collectionRepository.deleteById(id);
    }

    private ApiCollectionDto toDto(ApiCollection entity) {
        ApiCollectionDto dto = new ApiCollectionDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setNodeType(entity.getNodeType());
        dto.setParentId(entity.getParent() != null ? entity.getParent().getId() : null);
        dto.setMethod(entity.getMethod());
        dto.setUrl(entity.getUrl());
        dto.setParams(parseJson(entity.getParamsJson(), new TypeReference<>() {}));
        dto.setHeaders(parseJson(entity.getHeadersJson(), new TypeReference<>() {}));
        dto.setBodyType(entity.getBodyType());
        dto.setBodyRaw(entity.getBodyRaw());
        dto.setBodyRawType(entity.getBodyRawType());
        dto.setBodyForm(parseJson(entity.getBodyForm(), new TypeReference<>() {}));
        dto.setAuthType(entity.getAuthType());
        dto.setAuthConfig(parseJson(entity.getAuthConfig(), new TypeReference<>() {}));
        dto.setAssertions(parseJson(entity.getAssertions(), new TypeReference<>() {}));
        dto.setOrganizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null);
        dto.setProjectId(entity.getProject() != null ? entity.getProject().getId() : null);
        dto.setCreatorUsername(entity.getUser() != null ? entity.getUser().getUsername() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setChildren(null);
        return dto;
    }

    private ApiCollectionDto toDtoWithChildren(ApiCollection entity) {
        ApiCollectionDto dto = toDto(entity);
        if (entity.getNodeType() == ApiCollection.NodeType.FOLDER && entity.getChildren() != null) {
            dto.setChildren(entity.getChildren().stream()
                    .map(this::toDtoWithChildren)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private ApiCollection toEntity(ApiCollectionDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("名称不能为空");
        }
        ApiCollection collection = new ApiCollection();
        collection.setName(dto.getName().trim());
        collection.setDescription(dto.getDescription());
        
        ApiCollection.NodeType nodeType = dto.getNodeType();
        if (nodeType == null && dto.getType() != null) {
            try {
                nodeType = ApiCollection.NodeType.valueOf(dto.getType().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        collection.setNodeType(nodeType != null ? nodeType : ApiCollection.NodeType.FOLDER);
        
        collection.setMethod(dto.getMethod());
        collection.setUrl(dto.getUrl());
        collection.setParamsJson(toJson(dto.getParams()));
        collection.setHeadersJson(toJson(dto.getHeaders()));
        collection.setBodyType(dto.getBodyType());
        collection.setBodyRaw(dto.getBodyRaw());
        collection.setBodyRawType(dto.getBodyRawType());
        collection.setBodyForm(toJson(dto.getBodyForm()));
        collection.setAuthType(dto.getAuthType());
        collection.setAuthConfig(toJson(dto.getAuthConfig()));
        collection.setAssertions(toJson(dto.getAssertions()));
        return collection;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析失败: " + json, e);
        }
    }
}
