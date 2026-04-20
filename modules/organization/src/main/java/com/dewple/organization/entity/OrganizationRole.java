package com.dewple.organization.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.enums.OrganizationPermission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "organization_role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationRole extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "permissions", nullable = false)
    private Long permissions = 0L;

    @Column(name = "is_staff", nullable = false)
    private Boolean isStaff = false;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Builder
    public OrganizationRole(Organization organization, String name, Long permissions,
                            Boolean isStaff, Boolean isDefault) {
        this.organization = organization;
        this.name = name;
        this.permissions = permissions != null ? permissions : 0L;
        this.isStaff = isStaff != null ? isStaff : false;
        this.isDefault = isDefault != null ? isDefault : false;
    }

    public boolean hasPermission(OrganizationPermission permission) {
        return (this.permissions & permission.getValue()) != 0;
    }

    public void update(String name, Long permissions, Boolean isStaff) {
        this.name = name;
        this.permissions = permissions;
        this.isStaff = isStaff;
    }
}
