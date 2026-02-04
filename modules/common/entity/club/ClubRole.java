package com.dewple.entity;

import com.dewple.enums.Permission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "club_role"
//, uniqueConstraints = {@UniqueConstraint(columnNames = {"club_id", "name"})}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubRole extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "permissions", nullable = false)
    private Long permissions = 0L;

    @Builder
    public ClubRole(Club club, String name, Long permissions) {
        this.club = club;
        this.name = name;
        this.permissions = permissions != null ? permissions : 0L;
    }

    // 특정 권한을 가지고 있는지 확인
    public boolean hasPermission(Permission permission) {
        return (this.permissions & permission.getValue()) != 0;
    }

    // 권한 추가
    public void addPermission(Permission permission) {
        this.permissions |= permission.getValue();
    }

    // 권한 제거
    public void removePermission(Permission permission) {
        this.permissions &= ~permission.getValue();
    }

    // 여러 권한 한번에 추가
    public void addPermissions(Permission... permissions) {
        for (Permission permission : permissions) {
            addPermission(permission);
        }
    }

    // 모든 권한 부여
    public void grantAllPermissions() {
        this.permissions = Permission.all();
    }

    // 모든 권한 제거
    public void revokeAllPermissions() {
        this.permissions = 0L;
    }
}
