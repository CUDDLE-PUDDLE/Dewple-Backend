package com.dewple.activity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Category;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.Region;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.AttendanceCheckMethod;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.OpenType;
import com.dewple.organization.entity.Organization;

@Entity
@Table(name = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "open_type", nullable = false, length = 30)
    private OpenType openType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "is_attendance_check", nullable = false)
    private Boolean isAttendanceCheck = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_check_method", length = 20)
    private AttendanceCheckMethod attendanceCheckMethod;

    @Column(name = "is_searchable", nullable = false)
    private Boolean isSearchable = true;

    @Column(name = "start_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType = ActivityType.BOTH;

    @Column(name = "is_verification_required", nullable = false)
    private Boolean isVerificationRequired = false;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 30)
    private Gender gender = Gender.ANY;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Column(name = "invite_code", length = 36, unique = true)
    private String inviteCode;

    @Column(name = "emergency_contact", length = 100)
    private String emergencyContact;

    @Column(name = "cancel_deadline_days", nullable = false)
    private Integer cancelDeadlineDays = 1;

    @Column(name = "deadline_change_count", nullable = false)
    private Integer deadlineChangeCount = 0;

    @Column(name = "application_deadline", columnDefinition = "timestamptz")
    private OffsetDateTime applicationDeadline;

    @Column(name = "result_date", columnDefinition = "timestamptz")
    private OffsetDateTime resultDate;

    @Column(name = "has_application_form", nullable = false)
    private Boolean hasApplicationForm = false;

    @Builder
    public Activity(Club club, Organization organization, User creator, OpenType openType,
                    String name, String description, Integer capacity,
                    Boolean isAttendanceCheck, AttendanceCheckMethod attendanceCheckMethod,
                    Boolean isSearchable,
                    OffsetDateTime startAt, OffsetDateTime endAt,
                    Category category, Region region, ActivityType activityType,
                    Boolean isVerificationRequired, Integer minAge, Integer maxAge,
                    Gender gender, String thumbnailUrl, String inviteCode,
                    String emergencyContact, Integer cancelDeadlineDays,
                    OffsetDateTime applicationDeadline, OffsetDateTime resultDate,
                    Boolean hasApplicationForm) {
        this.club = club;
        this.organization = organization;
        this.creator = creator;
        this.openType = openType;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.isAttendanceCheck = isAttendanceCheck != null ? isAttendanceCheck : false;
        this.attendanceCheckMethod = attendanceCheckMethod;
        this.isSearchable = isSearchable != null ? isSearchable : true;
        this.startAt = startAt;
        this.endAt = endAt;
        this.category = category;
        this.region = region;
        this.activityType = activityType != null ? activityType : ActivityType.BOTH;
        this.isVerificationRequired = isVerificationRequired != null ? isVerificationRequired : false;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.gender = gender != null ? gender : Gender.ANY;
        this.thumbnailUrl = thumbnailUrl;
        this.inviteCode = inviteCode;
        this.emergencyContact = emergencyContact;
        this.cancelDeadlineDays = cancelDeadlineDays != null ? cancelDeadlineDays : 1;
        this.applicationDeadline = applicationDeadline;
        this.resultDate = resultDate;
        this.hasApplicationForm = hasApplicationForm != null ? hasApplicationForm : false;
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
