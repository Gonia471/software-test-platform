package com.testplatform.repository.apitest;

import com.testplatform.entity.apitest.ApiEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiEnvironmentRepository extends JpaRepository<ApiEnvironment, Long> {

    List<ApiEnvironment> findByOrganizationIdInOrderByIdAsc(List<Long> organizationIds);

    List<ApiEnvironment> findByUserIdAndOrganizationIsNullOrderByIdAsc(Long userId);
}
