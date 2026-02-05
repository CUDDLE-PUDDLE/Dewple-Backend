package com.dewple.club.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Club;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club1_id", nullable = false)
    private Club club1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club2_id", nullable = false)
    private Club club2;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    
    @Builder
    public ChatRoom(Club club1, Club club2, String name) {
        this.club1 = club1;
        this.club2 = club2;
        this.name = name;
    }
}
