package com.dewple.user.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true, length = 2048)
    private String token;

    @Column(name = "expire_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime expireAt;

    @Builder
    public RefreshToken(User user, String token, OffsetDateTime expireAt) {
        this.user = user;
        this.token = token;
        this.expireAt = expireAt;
    }

    public boolean isExpired() {
        return OffsetDateTime.now(ZoneOffset.UTC).isAfter(expireAt);
    }
}
