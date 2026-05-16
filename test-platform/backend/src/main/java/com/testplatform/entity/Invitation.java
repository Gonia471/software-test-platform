package com.testplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "invitations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"organization_id", "invitation_code"})
})
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Column(nullable = false, length = 32)
    private String invitationCode;

    @Column(length = 20)
    private String invitedPhone;

    @Column(nullable = false)
    private Boolean used = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_user_id")
    private User usedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant expiredAt;

    @Column(nullable = false)
    private Instant effectiveAt = Instant.now();

    public boolean isValid() {
        if (used != null && used) {
            return false;
        }
        if (expiredAt != null && Instant.now().isAfter(expiredAt)) {
            return false;
        }
        return true;
    }
}