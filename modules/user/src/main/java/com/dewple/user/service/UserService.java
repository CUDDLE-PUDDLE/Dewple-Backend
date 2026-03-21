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
                .birthdate(param.birthdate())
                .gender(param.gender())
                .build();

        userRepository.save(user);
        log.info("회원가입 완료: userId={}", param.userId());

        return user;
    }

    @Transactional
    public User login(LoginParam param) {
        User user = userRepository.findByUserId(param.userId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.isInDeletionPeriod()) {
            throw new BusinessException(UserErrorCode.USER_IN_DELETION);
        }

        if (user.getStatus() != BaseStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.USER_INACTIVE);
        }

        if (!passwordEncoderPort.matches(param.rawPassword(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
        }

        user.updateLastLoginAt();
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

        if (param.email() != null && userRepository.existsByEmailAndIdNot(param.email(), userId)) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (param.email() != null && !param.email().equals(user.getEmail())) {
            if (param.emailVerificationToken() == null) {
                throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_REQUIRED);
            }
            String verifiedEmail = verificationService.validateVerificationToken(param.emailVerificationToken());
            if (!verifiedEmail.equals(param.email())) {
                throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_REQUIRED);
            }
            user.markEmailVerified();
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

        List<String> interests = userCategoryRepository.findByUserIdWithCategory(userId).stream()
                .map(uc -> uc.getCategory().getName())
                .toList();

        log.info("프로필 수정 완료: userId={}", user.getUserId());
        return new MyProfileResult(user, interests);
    }

    @Transactional
    public void changeUserId(Long userId, String newUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getUserId() == null) {
            throw new BusinessException(UserErrorCode.KAKAO_ONLY_NO_USER_ID);
        }

        if (!user.canChangeUserId()) {
            throw new BusinessException(UserErrorCode.USER_ID_CHANGE_COOLDOWN);
        }

        if (userRepository.existsByUserId(newUserId)) {
            throw new BusinessException(UserErrorCode.USER_ID_ALREADY_EXISTS);
        }

        user.changeUserId(newUserId);
        log.info("아이디 변경 완료: userId={}", newUserId);
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

        user.markAsDeleted();

        // TODO: 참여 중 모임의 참여 확정 즉시 취소 (선착순 모임이면 대기자 자동 승격)
        // TODO: 모임장인 모임 처리 (모임관리자 있으면 승계, 없으면 모임 취소)
        // TODO: 회장인 동아리 처리 (부회장→권한 수 많은 운영진→랜덤 부원 순 승계)

        log.info("회원 탈퇴 요청 (소프트삭제): userId={}", user.getUserId());
    }

    @Transactional
    public void cancelWithdrawal(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (!user.isInDeletionPeriod()) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }

        user.cancelDeletion();

        // TODO: 탈퇴 취소 시 승계된 직위(회장직/모임장직)는 복원하지 않음
        // TODO: 참여 확정도 미복원 (재신청 필요)

        log.info("회원 탈퇴 취소: userId={}", user.getUserId());
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

        List<String> interests = userCategoryRepository.findByUserIdWithCategory(id).stream()
                .map(uc -> uc.getCategory().getName())
                .toList();

        String mbti = user.getMbti() != null ? user.getMbti().name() : null;

        return new UserProfileResult(
                user.getName(),
                user.getProfileImg(),
                user.getSelfIntroduction(),
                mbti,
                interests,
                user.getReputationScore()
        );
    }

    @Transactional(readOnly = true)
    public MyProfileResult getMyProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        List<String> interests = userCategoryRepository.findByUserIdWithCategory(id).stream()
                .map(uc -> uc.getCategory().getName())
                .toList();

        return new MyProfileResult(user, interests);
    }
}
