package com.dewple.user.service;

import com.dewple.common.entity.User;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.University;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.port.PasswordEncoderPort;
import com.dewple.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final PasswordEncoderPort passwordEncoderPort;

    @Transactional
    public User signup(String verificationToken, String name, String userId, String password) {
        String phone = verificationService.validateVerificationToken(verificationToken);

        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException(UserErrorCode.PHONE_ALREADY_EXISTS);
        }

        if (userRepository.existsByUserId(userId)) {
            throw new BusinessException(UserErrorCode.USER_ID_ALREADY_EXISTS);
        }

        User user = User.builder()
                .userId(userId)
                .password(passwordEncoderPort.encode(password))
                .name(name)
                .phone(phone)
                .build();

        userRepository.save(user);
        log.info("회원가입 완료: userId={}", userId);

        return user;
    }

    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId) {
        return !userRepository.existsByUserId(userId);
    }

    @Transactional
    public User updateProfile(Long id, String nickname, String email, LocalDate birthdate,
                              Gender gender, University university, Boolean isGraduated,
                              String workplace) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (nickname != null && userRepository.existsByNickname(nickname)) {
            throw new BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        if (email != null && userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.updateProfile(nickname, email, birthdate, gender, university, isGraduated, workplace);
        log.info("프로필 업데이트 완료: userId={}", user.getUserId());

        return user;
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}
