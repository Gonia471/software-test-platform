package com.testplatform.repository.uitest;

import com.testplatform.entity.uitest.UiTestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UiTestCategoryRepository extends JpaRepository<UiTestCategory, Long> {

    @Query("SELECT c FROM UiTestCategory c WHERE c.organization.id = :orgId ORDER BY c.displayName ASC, c.createdAt ASC")
    List<UiTestCategory> findAllByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT c FROM UiTestCategory c WHERE c.organization.id = :orgId AND c.moduleKey = :moduleKey")
    Optional<UiTestCategory> findByOrganizationIdAndModuleKey(@Param("orgId") Long orgId, @Param("moduleKey") String moduleKey);
}
