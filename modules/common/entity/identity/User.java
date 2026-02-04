package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.dewple.entity.enums.Gender;
import com.dewple.entity.enums.Mbti;
import com.dewple.entity.enums.Plan;
import com.dewple.entity.enums.University;

@Entity
@Table(name = "user")
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

    @Column(name = "profile_img", nullable = false, length = 255)
    private String profileImg;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "nickname", nullable = false, length = 255)
    private String nickname;

    @Column(name = "birthdate", nullable = false)
    private LocalDate birthdate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "email", nullable = false, length = 50)
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

    @Column(name = "self_introduction", nullable = false, columnDefinition = "TEXT")
    private String selfIntroduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "mbti", nullable = false)
    private Mbti mbti;

    @Column(name = "reputation_score", precision = 2, scale = 1)
    private BigDecimal reputationScore = BigDecimal.valueOf(5.0);


    @Builder
    public User(String userId, String password, String profileImg, String name, String nickname,
                LocalDate birthdate, Gender gender, String email, University university,
                Boolean isGraduated, String workplace, String phone, Plan plan,
                String selfIntroduction, Mbti mbti, BigDecimal reputationScore) {
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
        this.reputationScore = reputationScore != null ? reputationScore : BigDecimal.valueOf(5.0);
    }
}
