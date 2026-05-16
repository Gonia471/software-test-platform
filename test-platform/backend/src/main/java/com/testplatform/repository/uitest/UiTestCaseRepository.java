package com.testplatform.repository.uitest;

import com.testplatform.entity.uitest.UiTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UiTestCaseRepository extends JpaRepository<UiTestCase, Long> {

    @Query("SELECT c FROM UiTestCase c WHERE c.organization.id = :orgId ORDER BY c.updatedAt DESC")
    List<UiTestCase> findAllByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT c FROM UiTestCase c WHERE c.organization.id IN :orgIds ORDER BY c.updatedAt DESC")
    List<UiTestCase> findAllByOrganizationIdIn(@Param("orgIds") List<Long> orgIds);
}

