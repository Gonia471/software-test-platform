package com.testplatform.repository.apitest;

import com.testplatform.entity.apitest.ApiCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiCollectionRepository extends JpaRepository<ApiCollection, Long> {

    List<ApiCollection> findByUserIdOrderByIdAsc(Long userId);

    @Query("SELECT c FROM ApiCollection c WHERE c.user.id = :userId AND c.parent IS NULL ORDER BY c.id ASC")
    List<ApiCollection> findRootNodesByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM ApiCollection c WHERE c.user.id = :userId AND c.parent.id = :parentId ORDER BY c.id ASC")
    List<ApiCollection> findChildrenByParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    void deleteByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Query("SELECT c FROM ApiCollection c WHERE c.organization.id = :orgId AND c.parent IS NULL ORDER BY c.id ASC")
    List<ApiCollection> findRootNodesByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT c FROM ApiCollection c WHERE c.organization.id IN :orgIds AND c.parent IS NULL ORDER BY c.id ASC")
    List<ApiCollection> findRootNodesByOrganizationIdIn(@Param("orgIds") List<Long> orgIds);

    @Query("SELECT COUNT(c) FROM ApiCollection c WHERE c.organization.id = :orgId AND c.nodeType = :nodeType")
    long countByOrganizationIdAndNodeType(@Param("orgId") Long orgId, @Param("nodeType") ApiCollection.NodeType nodeType);

    @Query("SELECT c.id FROM ApiCollection c WHERE c.organization.id = :orgId AND c.nodeType = :nodeType")
    List<Long> findIdsByOrganizationIdAndNodeType(@Param("orgId") Long orgId, @Param("nodeType") ApiCollection.NodeType nodeType);

    @Query("SELECT c FROM ApiCollection c WHERE c.organization.id = :orgId AND c.nodeType = :nodeType ORDER BY c.updatedAt DESC")
    List<ApiCollection> findRecentByOrganizationIdAndNodeType(
            @Param("orgId") Long orgId,
            @Param("nodeType") ApiCollection.NodeType nodeType,
            Pageable pageable
    );
}
