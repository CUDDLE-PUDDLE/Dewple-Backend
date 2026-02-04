package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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


    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();

    
    @Builder
    public ChatRoom(Club club1, Club club2, String name) {
        this.club1 = club1;
        this.club2 = club2;
        this.name = name;
    }
}
