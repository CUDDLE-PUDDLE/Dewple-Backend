package com.dewple.user.service;

import com.dewple.common.entity.User;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.entity.PersonalFile;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.port.FileStoragePort;
import com.dewple.user.repository.PersonalFileRepository;
import com.dewple.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalFileService {

    private static final long FREE_STORAGE_BYTES = 1L * 1024 * 1024 * 1024; // 1GB

    private final PersonalFileRepository personalFileRepository;
    private final UserRepository userRepository;
    private final FileStoragePort fileStoragePort;

    @Transactional
    public PersonalFileResult uploadFile(Long userId, String originalName, long fileSize,
                                         String contentType, InputStream inputStream) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        long currentUsage = personalFileRepository.sumFileSizeByUserIdAndStatus(userId, BaseStatus.ACTIVE);

        // TODO: 초과 시 스토리지 추가 구매 기능 (과금 시스템 미구현)
        if (currentUsage + fileSize > FREE_STORAGE_BYTES) {
            throw new BusinessException(UserErrorCode.STORAGE_LIMIT_EXCEEDED);
        }

        String storedKey = String.format("personal/%d/%s_%s", userId, UUID.randomUUID(), originalName);
        fileStoragePort.upload(storedKey, inputStream, fileSize, contentType);

        PersonalFile file = PersonalFile.builder()
                .user(user)
                .originalName(originalName)
                .storedKey(storedKey)
                .fileSize(fileSize)
                .contentType(contentType)
                .build();

        personalFileRepository.save(file);
        log.info("개인 자료실 파일 업로드: userId={}, fileName={}, size={}", userId, originalName, fileSize);

        return PersonalFileResult.from(file);
    }

    @Transactional(readOnly = true)
    public List<PersonalFileResult> getFiles(Long userId) {
        return personalFileRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, BaseStatus.ACTIVE)
                .stream()
                .map(PersonalFileResult::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public String getDownloadUrl(Long userId, Long fileId) {
        PersonalFile file = personalFileRepository.findByIdAndUserId(fileId, userId)
                .filter(f -> f.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.FILE_NOT_FOUND));

        return fileStoragePort.generateDownloadUrl(file.getStoredKey());
    }

    @Transactional
    public void deleteFile(Long userId, Long fileId) {
        PersonalFile file = personalFileRepository.findByIdAndUserId(fileId, userId)
                .filter(f -> f.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.FILE_NOT_FOUND));

        file.inactivate();
        fileStoragePort.delete(file.getStoredKey());
        log.info("개인 자료실 파일 삭제: userId={}, fileId={}", userId, fileId);
    }

    @Transactional(readOnly = true)
    public StorageUsageResult getStorageUsage(Long userId) {
        long usedBytes = personalFileRepository.sumFileSizeByUserIdAndStatus(userId, BaseStatus.ACTIVE);
        return new StorageUsageResult(usedBytes, FREE_STORAGE_BYTES);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        List<PersonalFile> files = personalFileRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, BaseStatus.ACTIVE);

        for (PersonalFile file : files) {
            fileStoragePort.delete(file.getStoredKey());
        }

        personalFileRepository.deleteAll(files);
        log.info("개인 자료실 전체 삭제: userId={}, count={}", userId, files.size());
    }
}
