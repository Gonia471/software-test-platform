package com.testplatform.repository;

import com.testplatform.entity.EnterpriseSpace;
import com.testplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnterpriseSpaceRepository extends JpaRepository<EnterpriseSpace, Long> {

    @Query("SELECT es FROM EnterpriseSpace es JOIN es.members m WHERE m.user = :user ORDER BY es.createdAt ASC")
    List<EnterpriseSpace> findAllByMember(@Param("user") User user);

    @Query("SELECT es FROM EnterpriseSpace es LEFT JOIN FETCH es.members WHERE es.id = :id")
    Optional<EnterpriseSpace> findByIdWithMembers(@Param("id") Long id);
}
