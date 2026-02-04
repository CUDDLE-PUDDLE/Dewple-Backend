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
import com.dewple.common.enums.VerificationType;

@Entity
@Table(name = "verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "verificated_token", length = 2048)
    private String verificatedToken;

    @Column(name = "verification_data", nullable = false, length = 255)
    private String verificationData;

    @Column(name = "verification_code", nullable = false, length = 2048)
    private String verificationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 255)
    private VerificationType verificationType;

    @Column(name = "expire_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime expireAt;


    @Builder
    public Verification(User user, String verificatedToken, String verificationData,
                        String verificationCode, VerificationType verificationType,
                        OffsetDateTime expireAt) {
        this.publicId = UUID.randomUUID();
        this.user = user;
        this.verificatedToken = verificatedToken;
        this.verificationData = verificationData;
        this.verificationCode = verificationCode;
        this.verificationType = verificationType;
        this.expireAt = expireAt;
    }
}
