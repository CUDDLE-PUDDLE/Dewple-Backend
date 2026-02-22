package com.dewple.user.service;

import com.dewple.common.entity.User;
import com.dewple.user.entity.RefreshToken;
import com.dewple.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken save(User user, String token, long expirySeconds) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expireAt(OffsetDateTime.now().plusSeconds(expirySeconds))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
        log.info("전체 리프레시 토큰 삭제: userId={}", userId);
    }
}
