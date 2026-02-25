package com.dewple.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.University;

import java.math.BigDecimal;

@Entity
@Table(name = "club")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "university")
    private University university;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_img", length = 2048)
    private String coverImg;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "landing_page", columnDefinition = "jsonb")
    private String landingPage;

    @Column(name = "reputation_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal reputationScore = BigDecimal.valueOf(5.0);

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "min_age")
    private Long minAge;

    @Column(name = "max_age")
    private Long maxAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType = ActivityType.BOTH;


    @Builder
    public Club(User creator, University university, String name, String description,
                String coverImg, String landingPage, BigDecimal reputationScore,
                Gender gender, Long minAge, Long maxAge, ActivityType activityType) {
        this.creator = creator;
        this.university = university;
        this.name = name;
        this.description = description;
        this.coverImg = coverImg;
        this.landingPage = landingPage;
        this.reputationScore = reputationScore != null ? reputationScore : BigDecimal.valueOf(5.0);
        this.gender = gender;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.activityType = activityType != null ? activityType : ActivityType.BOTH;
    }
}
