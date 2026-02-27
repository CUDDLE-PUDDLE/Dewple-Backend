package com.dewple.user.service;

import com.dewple.common.entity.Category;
import com.dewple.common.entity.User;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.University;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.entity.UserCategory;
import com.dewple.user.port.PasswordEncoderPort;
import com.dewple.user.repository.CategoryRepository;
import com.dewple.user.repository.UserCategoryRepository;
import com.dewple.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final CategoryRepository categoryRepository;
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
    public User login(String userId, String rawPassword) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != BaseStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.USER_INACTIVE);
        }

        if (!passwordEncoderPort.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
        }

        log.info("로그인 성공: userId={}", userId);
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

    @Transactional
    public MyProfileResult editMyProfile(Long userId, EditMyProfileCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (command.nickname() != null && userRepository.existsByNicknameAndIdNot(command.nickname(), userId)) {
            throw new BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        if (command.email() != null && userRepository.existsByEmailAndIdNot(command.email(), userId)) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.editProfile(
                command.nickname(), command.email(), command.birthdate(),
                command.gender(), command.university(), command.isGraduated(),
                command.workplace(), command.profileImg(), command.selfIntroduction(),
                command.mbti()
        );

        if (command.categoryIds() != null) {
            userCategoryRepository.deleteByUserId(userId);

            if (!command.categoryIds().isEmpty()) {
                List<Category> categories = categoryRepository.findAllById(command.categoryIds());
                if (categories.size() != command.categoryIds().size()) {
                    throw new BusinessException(UserErrorCode.CATEGORY_NOT_FOUND);
                }

                List<UserCategory> userCategories = categories.stream()
                        .map(category -> UserCategory.builder()
                                .user(user)
                                .category(category)
                                .build())
                        .toList();
                userCategoryRepository.saveAll(userCategories);
            }
        }

        List<String> interests = userCategoryRepository.findByUserId(userId).stream()
                .map(uc -> uc.getCategory().getName())
                .toList();

        log.info("프로필 수정 완료: userId={}", user.getUserId());
        return new MyProfileResult(user, interests);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MyProfileResult getMyProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        List<String> interests = userCategoryRepository.findByUserId(id).stream()
                .map(uc -> uc.getCategory().getName())
                .toList();

        return new MyProfileResult(user, interests);
    }
}
