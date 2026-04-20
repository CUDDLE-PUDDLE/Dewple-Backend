package com.dewple.user.service;

import com.dewple.common.entity.Category;
import com.dewple.common.entity.User;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.common.exception.CommonErrorCode;import com.dewple.user.entity.UserCategory;
import com.dewple.user.port.PasswordEncoderPort;
import com.dewple.user.port.VerificationSendPort;
import com.dewple.user.port.WithdrawalActivityPort;
import com.dewple.user.port.WithdrawalClubPort;
import com.dewple.common.repository.CategoryRepository;
import com.dewple.user.repository.UserCategoryRepository;
import com.dewple.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
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
    private final VerificationSendPort verificationSendPort;
    private final WithdrawalActivityPort withdrawalActivityPort;
    private final WithdrawalClubPort withdrawalClubPort;
    private final RandomNicknameGenerator randomNicknameGenerator;
    private final PersonalFileService personalFileService;

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public User signup(SignupParam param) {
        String phone = verificationService.validateVerificationToken(param.verificationToken());

        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException(UserErrorCode.PHONE_ALREADY_EXISTS);
        }

        if (userRepository.existsByUserId(param.userId())) {
            throw new BusinessException(UserErrorCode.USER_ID_ALREADY_EXISTS);
        }

        String nickname = param.nickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = randomNicknameGenerator.generate();
        }

        User user = User.builder()
                .userId(param.userId())
                .password(passwordEncoderPort.encode(param.password()))
                .name(param.name())
                .nickname(nickname)
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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        // 소프트삭제 기간이 아닌 비활성 계정은 로그인 차단
        if (user.getStatus() != BaseStatus.ACTIVE && !user.isInDeletionPeriod()) {
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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

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
                    throw new BusinessException(CommonErrorCode.CATEGORY_NOT_FOUND);
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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != BaseStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.USER_INACTIVE);
        }

        // 1. deletedAt = now() 기록 + 소프트삭제
        user.markAsDeleted();

        // 2. 참여 중 모임의 참여 확정 즉시 취소
        withdrawalActivityPort.cancelConfirmedParticipations(userId);

        // 3. 모임장인 모임 처리 (모임관리자 있으면 승계, 없으면 모임 취소)
        withdrawalActivityPort.transferOrCancelLeaderActivities(userId);

        // 4. 회장인 동아리 처리 (운영진 → 권한 수 많은 순 → 일반 부원 순 승계)
        withdrawalClubPort.transferPresidentRoles(userId);

        // TODO: 1주일 경과 후 하드삭제 스케줄러 (app-worker 모듈에서 구현 필요)
        //   - 모든 동아리에서 탈퇴 처리 (이력에 탈퇴로 기록)
        //   - 게시물/댓글 유지 (유저명 → '(알 수 없음)')
        //   - 지원서 응답 삭제
        //   - 다른 참여자에게 부여한 별점은 유지

        log.info("회원 탈퇴 요청 (소프트삭제): userId={}", user.getUserId());
    }

    @Transactional
    public void cancelWithdrawal(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        if (!user.isInDeletionPeriod()) {
            throw new BusinessException(CommonErrorCode.USER_NOT_FOUND);
        }

        // 계정 복원 (단, 승계된 직위는 미복원)
        user.cancelDeletion();

        log.info("회원 탈퇴 취소: userId={}", user.getUserId());
    }

    @Transactional
    public void resetPassword(String verificationToken) {
        String phone = verificationService.validateVerificationToken(verificationToken);

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND_BY_PHONE));

        if (user.getStatus() != BaseStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.USER_INACTIVE);
        }

        // 카카오 전용 계정 (자체 아이디가 없는 경우)이면 안내
        if (user.getUserId() == null) {
            throw new BusinessException(UserErrorCode.KAKAO_ONLY_NO_PASSWORD);
        }

        String temporaryPassword = generateTemporaryPassword();
        user.changePassword(passwordEncoderPort.encode(temporaryPassword));

        verificationSendPort.sendTemporaryPassword(phone, temporaryPassword);
        log.info("임시 비밀번호 발급 완료: phone={}", phone.substring(0, phone.length() - 4) + "****");
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        // 최소 요건 보장: 대문자, 소문자, 숫자, 특수문자 각 1개
        sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYz".charAt(secureRandom.nextInt(26)));
        sb.append("abcdefghijklmnopqrstuvwxyz".charAt(secureRandom.nextInt(26)));
        sb.append("0123456789".charAt(secureRandom.nextInt(10)));
        sb.append("!@#$%^&*".charAt(secureRandom.nextInt(8)));
        // 나머지 랜덤 채우기
        for (int i = 4; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(secureRandom.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        // 셔플
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserProfileResult getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        List<String> interests = userCategoryRepository.findByUserIdWithCategory(id).stream()
                .map(uc -> uc.getCategory().getName())
                .toList();

        return new MyProfileResult(user, interests);
    }
}
