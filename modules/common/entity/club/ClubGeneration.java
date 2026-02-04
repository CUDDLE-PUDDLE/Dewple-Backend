package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "club_generation"
//, uniqueConstraints = { @UniqueConstraint(columnNames = {"club_id", "generation_no"})}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubGeneration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "generation_no", nullable = false)
    private Integer generationNo;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder
    public ClubGeneration(Club club, Integer generationNo, LocalDate startDate, LocalDate endDate) {
        this.club = club;
        this.generationNo = generationNo;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
