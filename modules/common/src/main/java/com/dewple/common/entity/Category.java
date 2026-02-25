package com.dewple.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    
    @Builder
    public Category(Category parent, String name) {
        this.parent = parent;
        this.name = name;
    }
}
