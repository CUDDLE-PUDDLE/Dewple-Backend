package com.dewple.user.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "personal_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "stored_key", nullable = false, length = 1000)
    private String storedKey;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Builder
    public PersonalFile(User user, String originalName, String storedKey,
                        Long fileSize, String contentType) {
        this.user = user;
        this.originalName = originalName;
        this.storedKey = storedKey;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }
}
