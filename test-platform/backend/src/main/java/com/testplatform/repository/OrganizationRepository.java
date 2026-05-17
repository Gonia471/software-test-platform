package com.testplatform.repository;

import com.testplatform.entity.Organization;
import com.testplatform.entity.OrganizationMember;
import com.testplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    @Query("SELECT DISTINCT o FROM Organization o JOIN o.enterpriseSpace.members m WHERE m.user = :user")
    List<Organization> findAllByMember(@Param("user") User user);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
           "FROM Organization o JOIN o.enterpriseSpace.members m " +
           "WHERE o.id = :orgId AND m.user.id = :userId")
    boolean isMember(@Param("orgId") Long orgId, @Param("userId") Long userId);

    @Query("SELECT m FROM OrganizationMember m WHERE m.organization.id = :orgId AND m.user = :user")
    Optional<OrganizationMember> findMember(@Param("orgId") Long orgId, @Param("user") User user);

    @Query("SELECT o FROM Organization o LEFT JOIN FETCH o.members LEFT JOIN FETCH o.enterpriseSpace WHERE o.id = :id")
    Optional<Organization> findByIdWithMembers(@Param("id") Long id);

    @Query("SELECT m FROM OrganizationMember m WHERE m.organization.id = :orgId")
    List<OrganizationMember> findAllMembersByOrganizationId(@Param("orgId") Long orgId);
}
