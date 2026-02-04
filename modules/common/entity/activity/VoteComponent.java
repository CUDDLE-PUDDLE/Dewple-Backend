package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vote_component")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteComponent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    
    @Builder
    public VoteComponent(Vote vote, String content) {
        this.vote = vote;
        this.content = content;
    }
}
