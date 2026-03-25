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
import com.dewple.common.enums.ApprovalStatus;
import com.dewple.common.enums.ContactPreference;
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

    @Column(name = "purpose", length = 1000)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_preference", length = 10)
    private ContactPreference contactPreference;

    @Column(name = "target_clubs_description", length = 500)
    private String targetClubsDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_ids", columnDefinition = "jsonb")
    private String categoryIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "region_ids", columnDefinition = "jsonb")
    private String regionIds;


    @Builder
    public Organization(User creator, String name, String description,
                        String coverImg, String landingPage, Boolean isPublic,
                        OrganizationType type, ActivityType activityType, LocalDate foundedDate,
                        String purpose, String contactEmail, String contactPhone,
                        ContactPreference contactPreference, String targetClubsDescription,
                        String categoryIds, String regionIds) {
        this.creator = creator;
        this.name = name;
        this.description = description;
        this.coverImg = coverImg;
        this.landingPage = landingPage;
        this.isPublic = isPublic != null ? isPublic : true;
        this.type = type;
        this.activityType = activityType != null ? activityType : ActivityType.BOTH;
        this.foundedDate = foundedDate;
        this.purpose = purpose;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.contactPreference = contactPreference;
        this.targetClubsDescription = targetClubsDescription;
        this.categoryIds = categoryIds;
        this.regionIds = regionIds;
    }

    public void update(String name, String description, String coverImg,
                       OrganizationType type, ActivityType activityType,
                       String purpose, String contactEmail, String contactPhone,
                       ContactPreference contactPreference, String targetClubsDescription,
                       String categoryIds, String regionIds) {
        this.name = name;
        this.description = description;
        this.coverImg = coverImg;
        this.type = type;
        this.activityType = activityType;
        this.purpose = purpose;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.contactPreference = contactPreference;
        this.targetClubsDescription = targetClubsDescription;
        this.categoryIds = categoryIds;
        this.regionIds = regionIds;
    }

    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    public void reject(String reason) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.rejectionReason = reason;
    }
}
