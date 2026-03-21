package com.dewple.organization.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.OrganizationType;

import java.time.LocalDate;

@Entity
@Table(name = "organization")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization extends BaseEntity {

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

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private OrganizationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType = ActivityType.BOTH;

    @Column(name = "founded_date")
    private LocalDate foundedDate;


    @Builder
    public Organization(User creator, String name, String description,
                        String coverImg, String landingPage, Boolean isPublic,
                        OrganizationType type, ActivityType activityType, LocalDate foundedDate) {
        this.creator = creator;
        this.name = name;
        this.description = description;
        this.coverImg = coverImg;
        this.landingPage = landingPage;
        this.isPublic = isPublic != null ? isPublic : true;
        this.type = type;
        this.activityType = activityType != null ? activityType : ActivityType.BOTH;
        this.foundedDate = foundedDate;
    }
}
