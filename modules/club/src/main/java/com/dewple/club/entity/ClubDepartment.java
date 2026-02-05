package com.dewple.club.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Club;

@Entity
@Table(name = "club_department")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubDepartment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "name", nullable = false, length = 100)
    private String name;


    @Builder
    public ClubDepartment(Club club, String name) {
        this.club = club;
        this.name = name;
    }
}
