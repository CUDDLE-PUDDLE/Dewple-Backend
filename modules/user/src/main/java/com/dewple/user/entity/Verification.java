package com.dewple.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import com.dewple.common.enums.VerificationPurpose;
import com.dewple.common.enums.VerificationType;

@Entity
@Table(name = "verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification extends BaseEntity {

    public static final int CODE_EXPIRATION_MINUTES = 3;
    public static final int TOKEN_VALID_MINUTES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "verification_token", length = 2048)
    private String token;

    @Column(name = "verification_target", nullable = false, length = 255)
    private String target;

    @Column(name = "verification_code", nullable = false, length = 2048)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 255)
    private VerificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    private VerificationPurpose purpose;

    @Column(name = "verification_token_expire_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime tokenExpireAt;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Builder
    public Verification(User user, VerificationType type,
                        String target, String code,
                        VerificationPurpose purpose) {
        this.publicId = UUID.randomUUID();
        this.user = user;
        this.type = type;
        this.target = target;
        this.code = code;
        this.purpose = purpose;
        this.tokenExpireAt = OffsetDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);
        this.isVerified = false;
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(tokenExpireAt);
    }

    public boolean verifyCode(String inputCode) {
        return this.code.equals(inputCode) && !isExpired();
    }

    public void markAsVerified() {
        this.isVerified = true;
        this.token = "vp_" + UUID.randomUUID();
        this.tokenExpireAt = OffsetDateTime.now().plusMinutes(TOKEN_VALID_MINUTES);
    }

    public boolean isTokenValid() {
        return isVerified
                && token != null
                && tokenExpireAt != null
                && OffsetDateTime.now().isBefore(tokenExpireAt);
    }
}
