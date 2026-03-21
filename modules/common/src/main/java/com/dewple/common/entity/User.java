package com.dewple.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dewple.common.enums.Gender;
import com.dewple.common.enums.Mbti;
import com.dewple.common.enums.Plan;
import com.dewple.common.enums.University;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "profile_img", length = 255)
    private String profileImg;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "nickname", length = 255)
    private String nickname;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "email", length = 50)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "university")
    private University university;

    @Column(name = "is_graduated")
    private Boolean isGraduated;

    @Column(name = "workplace", length = 100)
    private String workplace;

    @Column(name = "phone", nullable = false, unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private Plan plan = Plan.FREE;

    @Column(name = "self_introduction", columnDefinition = "TEXT")
    private String selfIntroduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "mbti")
    private Mbti mbti;

    @Column(name = "reputation_score", precision = 2, scale = 1)
    private BigDecimal reputationScore;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "last_login_at", columnDefinition = "timestamptz")
    private OffsetDateTime lastLoginAt;

    @Column(name = "user_id_changed_at", columnDefinition = "timestamptz")
    private OffsetDateTime userIdChangedAt;

    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private OffsetDateTime deletedAt;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;


    @Builder
    public User(String userId, String password, String profileImg, String name, String nickname,
                LocalDate birthdate, Gender gender, String email, University university,
                Boolean isGraduated, String workplace, String phone, Plan plan,
                String selfIntroduction, Mbti mbti, BigDecimal reputationScore,
                Boolean isVerified, Boolean isEmailVerified) {
        this.userId = userId;
        this.password = password;
        this.profileImg = profileImg;
        this.name = name;
        this.nickname = nickname;
        this.birthdate = birthdate;
        this.gender = gender;
        this.email = email;
        this.university = university;
        this.isGraduated = isGraduated;
        this.workplace = workplace;
        this.phone = phone;
        this.plan = plan != null ? plan : Plan.FREE;
        this.selfIntroduction = selfIntroduction;
        this.mbti = mbti;
        this.reputationScore = reputationScore;
        this.isVerified = isVerified != null ? isVerified : false;
        this.isEmailVerified = isEmailVerified != null ? isEmailVerified : false;
    }

    public void updateProfile(String nickname, String email, LocalDate birthdate,
                              Gender gender, University university, Boolean isGraduated,
                              String workplace) {
        this.nickname = nickname;
        this.email = email;
        this.birthdate = birthdate;
        this.gender = gender;
        this.university = university;
        this.isGraduated = isGraduated;
        this.workplace = workplace;
    }

    public void changePhone(String phone) {
        this.phone = phone;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = OffsetDateTime.now();
    }

    public void markAsDeleted() {
        this.deletedAt = OffsetDateTime.now();
        this.inactivate();
    }

    public void cancelDeletion() {
        this.deletedAt = null;
        this.activate();
    }

    public boolean isInDeletionPeriod() {
        return this.deletedAt != null
                && OffsetDateTime.now().isBefore(this.deletedAt.plusDays(7));
    }

    public void changeUserId(String newUserId) {
        this.userId = newUserId;
        this.userIdChangedAt = OffsetDateTime.now();
    }

    public boolean canChangeUserId() {
        if (this.userIdChangedAt == null) return true;
        return OffsetDateTime.now().isAfter(this.userIdChangedAt.plusDays(7));
    }

    public void markEmailVerified() {
        this.isEmailVerified = true;
    }

    public void editProfile(String nickname, String email, LocalDate birthdate,
                            Gender gender, University university, Boolean isGraduated,
                            String workplace, String profileImg, String selfIntroduction,
                            Mbti mbti) {
        if (nickname != null) this.nickname = nickname;
        if (email != null) this.email = email;
        if (birthdate != null) this.birthdate = birthdate;
        if (gender != null) this.gender = gender;
        if (university != null) this.university = university;
        if (isGraduated != null) this.isGraduated = isGraduated;
        if (workplace != null) this.workplace = workplace;
        if (profileImg != null) this.profileImg = profileImg;
        if (selfIntroduction != null) this.selfIntroduction = selfIntroduction;
        if (mbti != null) this.mbti = mbti;
    }
}
