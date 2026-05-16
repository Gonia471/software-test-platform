package com.testplatform.repository;

import com.testplatform.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {
    
    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);
    
    @Query("SELECT COUNT(m) FROM OrganizationMember m WHERE m.organization.id = :orgId")
    int countByOrganizationId(@Param("orgId") Long orgId);
}