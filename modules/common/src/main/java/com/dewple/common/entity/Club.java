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

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @Column(name = "is_verification_required", nullable = false)
    private Boolean isVerificationRequired = false;

    @Column(name = "founded_date")
    private LocalDate foundedDate;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;


    @Builder
    public Club(User creator, String name, String description,
                String coverImg, String landingPage, BigDecimal reputationScore,
                Gender gender, Long minAge, Long maxAge, ActivityType activityType,
                Boolean isHidden, Boolean isVerificationRequired, LocalDate foundedDate) {
        this.creator = creator;
        this.name = name;
        this.description = description;
        this.coverImg = coverImg;
        this.landingPage = landingPage;
        this.reputationScore = reputationScore != null ? reputationScore : BigDecimal.valueOf(5.0);
        this.gender = gender;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.activityType = activityType != null ? activityType : ActivityType.BOTH;
        this.isHidden = isHidden != null ? isHidden : false;
        this.isVerificationRequired = isVerificationRequired != null ? isVerificationRequired : false;
        this.foundedDate = foundedDate;
    }

    public void update(String name, String description, String coverImg,
                       ActivityType activityType, LocalDate foundedDate) {
        this.name = name;
        this.description = description;
        this.coverImg = coverImg;
        this.activityType = activityType;
        this.foundedDate = foundedDate;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
