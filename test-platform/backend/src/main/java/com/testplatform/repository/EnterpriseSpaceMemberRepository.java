package com.testplatform.repository;

import com.testplatform.entity.EnterpriseSpaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnterpriseSpaceMemberRepository extends JpaRepository<EnterpriseSpaceMember, Long> {

    Optional<EnterpriseSpaceMember> findByEnterpriseSpaceIdAndUserId(Long enterpriseSpaceId, Long userId);

    List<EnterpriseSpaceMember> findAllByEnterpriseSpaceId(Long enterpriseSpaceId);

    boolean existsByEnterpriseSpaceIdAndUserId(Long enterpriseSpaceId, Long userId);
}
