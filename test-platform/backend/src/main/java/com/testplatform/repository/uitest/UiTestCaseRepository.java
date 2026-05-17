package com.testplatform.repository.uitest;

import com.testplatform.entity.uitest.UiTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UiTestCaseRepository extends JpaRepository<UiTestCase, Long> {

    @Query("SELECT c FROM UiTestCase c WHERE c.organization.id = :orgId ORDER BY c.updatedAt DESC")
    List<UiTestCase> findAllByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT c FROM UiTestCase c WHERE c.organization.id IN :orgIds ORDER BY c.updatedAt DESC")
    List<UiTestCase> findAllByOrganizationIdIn(@Param("orgIds") List<Long> orgIds);

    @Query("SELECT COUNT(c) FROM UiTestCase c WHERE c.organization.id = :orgId")
    long countByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT c.id FROM UiTestCase c WHERE c.organization.id = :orgId")
    List<Long> findIdsByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT c FROM UiTestCase c WHERE c.organization.id = :orgId ORDER BY c.updatedAt DESC")
    List<UiTestCase> findRecentByOrganizationId(@Param("orgId") Long orgId, Pageable pageable);

    @Query("SELECT DISTINCT c.moduleKey FROM UiTestCase c WHERE c.organization.id = :orgId AND c.moduleKey IS NOT NULL AND TRIM(c.moduleKey) <> ''")
    List<String> findDistinctModuleKeysByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(c) FROM UiTestCase c WHERE c.organization.id = :orgId AND c.moduleKey = :moduleKey")
    long countByOrganizationIdAndModuleKey(@Param("orgId") Long orgId, @Param("moduleKey") String moduleKey);
}

