package com.dewple.user.service;

import com.dewple.user.entity.PersonalFile;

import java.time.OffsetDateTime;

public record PersonalFileResult(
        Long id,
        String originalName,
        Long fileSize,
        String contentType,
        OffsetDateTime createdAt
) {
    public static PersonalFileResult from(PersonalFile file) {
        return new PersonalFileResult(
                file.getId(),
                file.getOriginalName(),
                file.getFileSize(),
                file.getContentType(),
                file.getCreatedAt()
        );
    }
}
