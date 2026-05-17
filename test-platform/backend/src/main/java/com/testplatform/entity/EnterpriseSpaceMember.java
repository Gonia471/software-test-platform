package com.testplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "enterprise_space_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"enterprise_space_id", "user_id"})
})
public class EnterpriseSpaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_space_id", nullable = false)
    private EnterpriseSpace enterpriseSpace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.MEMBER;

    @Column(nullable = false)
    private Boolean canRead = true;

    @Column(nullable = false)
    private Boolean canWrite = false;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    public enum Role {
        SPACE_CREATOR,
        SPACE_ADMIN,
        MEMBER
    }

    public boolean isSpaceCreator() {
        return this.role == Role.SPACE_CREATOR;
    }

    public boolean isSpaceAdmin() {
        return this.role == Role.SPACE_CREATOR || this.role == Role.SPACE_ADMIN;
    }
}
