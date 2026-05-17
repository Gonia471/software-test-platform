package com.testplatform.repository;

import com.testplatform.entity.Invitation;
import com.testplatform.entity.EnterpriseSpace;
import com.testplatform.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByInvitationCode(String invitationCode);

    List<Invitation> findByOrganization(Organization organization);

    List<Invitation> findByEnterpriseSpace(EnterpriseSpace enterpriseSpace);

    List<Invitation> findByUsedById(Long userId);

    @Query("SELECT i FROM Invitation i WHERE i.invitedPhone = :phone AND i.used = false ORDER BY i.createdAt DESC")
    List<Invitation> findPendingInvitationsByPhone(@Param("phone") String phone);

    boolean existsByInvitationCode(String invitationCode);
}
