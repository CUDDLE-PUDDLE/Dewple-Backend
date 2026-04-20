package com.dewple.organization.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "organization_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private OrganizationRole role;

    @Builder
    public OrganizationMember(Organization organization, User user, OrganizationRole role) {
        this.organization = organization;
        this.user = user;
        this.role = role;
    }

    public void changeRole(OrganizationRole role) {
        this.role = role;
    }
}
