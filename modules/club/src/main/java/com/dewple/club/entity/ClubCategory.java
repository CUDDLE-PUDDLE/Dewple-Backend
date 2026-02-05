package com.dewple.club.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Category;
import com.dewple.common.entity.Club;

@Entity
@Table(name = "club_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder
    public ClubCategory(Club club, Category category) {
        this.club = club;
        this.category = category;
    }
}
