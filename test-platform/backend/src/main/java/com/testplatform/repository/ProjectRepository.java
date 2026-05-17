package com.testplatform.repository;

import com.testplatform.entity.Organization;
import com.testplatform.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    interface ProjectListProjection {
        Long getId();
        String getName();
        String getDescription();
        String getColor();
        Long getOrganizationId();
        String getOrganizationName();
        Long getOwnerId();
        String getOwnerUsername();
        String getType();
        String getItemsJson();
        String getCronExpression();
        Integer getLoopCount();
        Long getUiInstanceId();
        Boolean getEnabled();
        java.time.Instant getCreatedAt();
        java.time.Instant getUpdatedAt();
    }

    List<Project> findAllByOrganizationOrderByCreatedAtDesc(Organization organization);

    @Query("SELECT p FROM Project p JOIN FETCH p.organization JOIN FETCH p.owner WHERE p.organization.id = :orgId ORDER BY p.createdAt DESC")
    List<Project> findAllByOrganizationId(@Param("orgId") Long orgId);

    @Query("""
            SELECT
                p.id AS id,
                p.name AS name,
                p.description AS description,
                p.color AS color,
                o.id AS organizationId,
                o.name AS organizationName,
                owner.id AS ownerId,
                owner.username AS ownerUsername,
                p.type AS type,
                p.itemsJson AS itemsJson,
                p.cronExpression AS cronExpression,
                p.loopCount AS loopCount,
                p.uiInstanceId AS uiInstanceId,
                p.enabled AS enabled,
                p.createdAt AS createdAt,
                p.updatedAt AS updatedAt
            FROM Project p
            JOIN p.organization o
            JOIN p.owner owner
            WHERE o.id = :orgId
            ORDER BY p.createdAt DESC
            """)
    List<ProjectListProjection> findProjectListByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT p FROM Project p JOIN FETCH p.organization JOIN FETCH p.owner WHERE p.id = :id")
    Optional<Project> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT p FROM Project p JOIN FETCH p.organization JOIN FETCH p.owner WHERE p.type = :type AND p.organization.id = :orgId ORDER BY p.createdAt DESC")
    List<Project> findAllByOrganizationIdAndType(@Param("orgId") Long orgId, @Param("type") String type);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM OrganizationMember m " +
           "WHERE m.organization.id = :orgId AND m.user.id = :userId")
    boolean isOrgMember(@Param("orgId") Long orgId, @Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.organization.id = :orgId")
    int countByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.organization.id = :orgId AND p.type = :type")
    int countByOrganizationIdAndType(@Param("orgId") Long orgId, @Param("type") String type);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.organization.id = :orgId AND p.enabled = true")
    int countEnabledByOrganizationId(@Param("orgId") Long orgId);

    @Query("""
            SELECT COUNT(p)
            FROM Project p
            WHERE p.organization.id = :orgId
              AND p.enabled = true
              AND p.cronExpression IS NOT NULL
              AND TRIM(p.cronExpression) <> ''
            """)
    int countScheduledByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT p.id FROM Project p WHERE p.organization.id = :orgId")
    List<Long> findIdsByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT p FROM Project p JOIN FETCH p.owner WHERE p.organization.id = :orgId ORDER BY p.updatedAt DESC")
    List<Project> findRecentByOrganizationId(@Param("orgId") Long orgId, Pageable pageable);
}
