package com.dewple.user.service;

import com.dewple.common.entity.Category;
import com.dewple.common.entity.User;
import com.dewple.common.enums.BaseStatus;
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
    public User signup(SignupParam param) {
        String phone = verificationService.validateVerificationToken(param.verificationToken());

        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException(UserErrorCode.PHONE_ALREADY_EXISTS);
        }

        if (userRepository.existsByUserId(param.userId())) {
            throw new BusinessException(UserErrorCode.USER_ID_ALREADY_EXISTS);
        }

        User user = User.builder()
                .userId(param.userId())
                .password(passwordEncoderPort.encode(param.password()))
                .name(param.name())
                .phone(phone)
                .build();

        userRepository.save(user);
        log.info("회원가입 완료: userId={}", param.userId());

        return user;
    }

    @Transactional(readOnly = true)
    public User login(LoginParam param) {
        User user = userRepository.findByUserId(param.userId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != BaseStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.USER_INACTIVE);
        }

        if (!passwordEncoderPort.matches(param.rawPassword(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
        }

        log.info("로그인 성공: userId={}", param.userId());
        return user;
    }

    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId) {
        return !userRepository.existsByUserId(userId);
    }

    @Transactional
    public User updateProfile(Long id, UpdateProfileParam param) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (param.nickname() != null && userRepository.existsByNickname(param.nickname())) {
            throw new BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        if (param.email() != null && userRepository.existsByEmail(param.email())) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.updateProfile(param.nickname(), param.email(), param.birthdate(),
                param.gender(), param.university(), param.isGraduated(), param.workplace());
        log.info("프로필 업데이트 완료: userId={}", user.getUserId());

        return user;
    }

    @Transactional
    public MyProfileResult editMyProfile(Long userId, EditMyProfileParam param) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (param.nickname() != null && userRepository.existsByNicknameAndIdNot(param.nickname(), userId)) {
            throw new BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        if (param.email() != null && userRepository.existsByEmailAndIdNot(param.email(), userId)) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.editProfile(
                param.nickname(), param.email(), param.birthdate(),
                param.gender(), param.university(), param.isGraduated(),
                param.workplace(), param.profileImg(), param.selfIntroduction(),
                param.mbti()
        );

        if (param.categoryIds() != null) {
            userCategoryRepository.deleteByUserId(userId);

            if (!param.categoryIds().isEmpty()) {
                List<Category> categories = categoryRepository.findAllById(param.categoryIds());
                if (categories.size() != param.categoryIds().size()) {
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

    @Transactional
    public void changePhone(Long userId, String verificationToken) {
        String newPhone = verificationService.validateVerificationToken(verificationToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (newPhone.equals(user.getPhone())) {
            throw new BusinessException(UserErrorCode.PHONE_SAME_AS_CURRENT);
        }

        if (userRepository.existsByPhone(newPhone)) {
            throw new BusinessException(UserErrorCode.PHONE_ALREADY_EXISTS);
        }

        user.changePhone(newPhone);
        log.info("전화번호 변경 완료: userId={}", user.getUserId());
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != BaseStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.USER_INACTIVE);
        }

        user.inactivate();
        log.info("회원 탈퇴 완료: userId={}", user.getUserId());
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserProfileResult getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        List<String> interests = userCategoryRepository.findByUserId(id).stream()
                .map(uc -> uc.getCategory().getName())
                .toList();

        String mbti = user.getMbti() != null ? user.getMbti().name() : null;

        return new UserProfileResult(
                user.getName(),
                user.getProfileImg(),
                user.getSelfIntroduction(),
                mbti,
                interests
        );
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
