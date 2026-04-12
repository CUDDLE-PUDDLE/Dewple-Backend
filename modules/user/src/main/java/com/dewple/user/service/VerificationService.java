package com.dewple.user.service;

import com.dewple.common.enums.VerificationPurpose;
import com.dewple.common.enums.VerificationType;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.entity.Verification;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.port.EmailVerificationPort;
import com.dewple.user.port.SmsVerificationPort;
import com.dewple.user.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final int REQUEST_COOLDOWN_SECONDS = 60;
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    private final VerificationRepository verificationRepository;
    private final SmsVerificationPort smsVerificationPort;
    private final EmailVerificationPort emailVerificationPort;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Verification sendPhoneVerificationCode(String phone, VerificationPurpose purpose) {
        String normalizedPhone = phone.replace("-", "");

        validateRequestCooldown(normalizedPhone, VerificationType.PHONE);

        String verificationCode = generateVerificationCode();

        Verification verification = Verification.builder()
                .type(VerificationType.PHONE)
                .target(normalizedPhone)
                .code(verificationCode)
                .purpose(purpose)
                .build();

        verificationRepository.save(verification);

        try {
            smsVerificationPort.sendVerificationCode(normalizedPhone, verificationCode);
            log.info("SMS 인증 코드 발송 완료: phone={}", maskPhone(normalizedPhone));
        } catch (Exception e) {
            log.error("SMS 발송 실패: phone={}", maskPhone(normalizedPhone), e);
            throw new BusinessException(UserErrorCode.SMS_SEND_FAILED, e);
        }

        return verification;
    }

    @Transactional
    public Verification sendEmailVerificationCode(String email, VerificationPurpose purpose) {
        validateRequestCooldown(email, VerificationType.EMAIL);

        String verificationCode = generateVerificationCode();

        Verification verification = Verification.builder()
                .type(VerificationType.EMAIL)
                .target(email)
                .code(verificationCode)
                .purpose(purpose)
                .build();

        verificationRepository.save(verification);

        try {
            emailVerificationPort.sendVerificationCode(email, verificationCode);
            log.info("이메일 인증 코드 발송 완료: email={}", maskEmail(email));
        } catch (Exception e) {
            log.error("이메일 발송 실패: email={}", maskEmail(email), e);
            throw new BusinessException(UserErrorCode.EMAIL_SEND_FAILED, e);
        }

        return verification;
    }

    @Transactional
    public Verification verifyPhoneCode(String verificationId, String code) {
        Verification verification = verificationRepository.findByPublicId(UUID.fromString(verificationId))
                .orElseThrow(() -> new BusinessException(UserErrorCode.VERIFICATION_NOT_FOUND));

        if (verification.isTokenValid()) {
            return verification;
        }

        if (verification.isExpired()) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!verification.verifyCode(code)) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_INVALID);
        }

        verification.markAsVerified();
        log.info("전화번호 인증 완료: verificationId={}", verificationId);

        return verification;
    }

    @Transactional(readOnly = true)
    public String validateVerificationToken(String verificationToken) {
        Verification verification = verificationRepository.findByToken(verificationToken)
                .orElseThrow(() -> new BusinessException(UserErrorCode.VERIFICATION_TOKEN_INVALID));

        if (!verification.isTokenValid()) {
            throw new BusinessException(UserErrorCode.VERIFICATION_TOKEN_EXPIRED);
        }

        return verification.getTarget();
    }

    private void validateRequestCooldown(String data, VerificationType type) {
        verificationRepository.findLatestByDataAndType(data, type)
                .ifPresent(verification -> {
                    OffsetDateTime cooldownEnd = verification.getCreatedAt()
                            .plusSeconds(REQUEST_COOLDOWN_SECONDS);

                    if (OffsetDateTime.now(ZoneOffset.UTC).isBefore(cooldownEnd)) {
                        throw new BusinessException(UserErrorCode.TOO_MANY_VERIFICATION_REQUESTS);
                    }
                });
    }

    private String generateVerificationCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHANUMERIC_CHARS.charAt(secureRandom.nextInt(ALPHANUMERIC_CHARS.length())));
        }
        return sb.toString();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) {
            return "**@" + parts[1];
        }
        return local.substring(0, 2) + "****@" + parts[1];
    }
}
