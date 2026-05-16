package com.testplatform.repository;

import com.testplatform.entity.OrgConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrgConfigRepository extends JpaRepository<OrgConfig, Long> {
    Optional<OrgConfig> findByOrganizationId(Long organizationId);
    boolean existsByOrganizationId(Long organizationId);
}