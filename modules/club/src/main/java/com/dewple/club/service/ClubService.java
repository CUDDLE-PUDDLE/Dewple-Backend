package com.dewple.club.service;

import com.dewple.club.entity.*;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.port.ClubRecruitmentPort;
import com.dewple.club.repository.*;
import com.dewple.common.entity.Category;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.Region;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ClubDeletionStatus;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import com.dewple.common.exception.CommonErrorCode;
import com.dewple.common.repository.CategoryRepository;
import com.dewple.common.repository.RegionRepository;
import com.dewple.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubRoleRepository clubRoleRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubCategoryRepository clubCategoryRepository;
    private final ClubRegionRepository clubRegionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;
    private final ClubRecruitmentPort clubRecruitmentPort;
    private final ClubDeletionVoteRepository clubDeletionVoteRepository;
    private final ClubGenerationRepository clubGenerationRepository;
    private final ClubKickVoteRepository clubKickVoteRepository;

    private static final String PRESIDENT_ROLE_NAME = "회장";
    private static final int MAX_PRESIDENT_CLUBS = 5;

    @Transactional
    public CreateClubResult createClub(Long userId, CreateClubParam param) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        validateCreateClub(userId, param);

        Club club = Club.builder()
                .creator(creator)
                .name(param.name())
                .activityType(param.activityType())
                .isVerificationRequired(param.isVerificationRequired())
                .foundedDate(param.foundedDate())
                .gender(Gender.ANY)
                .build();
        clubRepository.save(club);

        saveClubCategories(club, param.categoryIds());
        saveClubRegions(club, param.regionIds());
        initializeDefaultRoles(club);

        ClubRole presidentRole = clubRoleRepository.findByClubIdAndName(club.getId(), PRESIDENT_ROLE_NAME)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        ClubMember creatorMember = ClubMember.builder()
                .club(club)
                .user(creator)
                .role(presidentRole)
                .activityStatus(ActivityStatus.ACTIVE)
                .build();
        clubMemberRepository.save(creatorMember);

        log.info("동아리 생성: clubId={}, userId={}", club.getId(), userId);

        return new CreateClubResult(
                club.getId(), club.getName(), club.getIsVerificationRequired(),
                club.getActivityType(), club.getFoundedDate(),
                param.categoryIds(), param.regionIds(), userId
        );
    }

    @Transactional(readOnly = true)
    public ClubDetailResult getClubDetail(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));
        return ClubDetailResult.from(club);
    }

    @Transactional
    public ClubMemberResult inviteGuest(Long userId, Long clubId, InviteGuestParam param) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        validateClubPermission(clubId, userId, Permission.MANAGE_MEMBER);

        if (param.activityIds() == null || param.activityIds().isEmpty()) {
            throw new BusinessException(ClubErrorCode.ACTIVITY_ID_REQUIRED);
        }

        if (clubMemberRepository.findByClubIdAndUserId(clubId, param.userId()).isPresent()) {
            throw new BusinessException(ClubErrorCode.ALREADY_CLUB_MEMBER);
        }

        User guestUser = userRepository.findById(param.userId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        ClubRole defaultMemberRole = clubRoleRepository.findByClubIdAndName(clubId, "부원")
                .orElseThrow(() -> new BusinessException(ClubErrorCode.ROLE_NOT_FOUND));

        ClubMember guest = ClubMember.builder()
                .club(club)
                .user(guestUser)
                .role(defaultMemberRole)
                .activityStatus(ActivityStatus.GUEST)
                .build();
        clubMemberRepository.save(guest);

        // TODO: activityIds로 모임 연결 (Activity 도메인 통합 시 구현)
        log.info("동아리 GUEST 초대: clubId={}, guestUserId={}, activityIds={}",
                clubId, param.userId(), param.activityIds());

        return ClubMemberResult.from(guest);
    }

    @Transactional
    public void promoteToMember(Long userId, Long clubId, Long memberId, PromoteToMemberParam param) {
        clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        validateClubPermission(clubId, userId, Permission.MANAGE_MEMBER);

        ClubMember member = clubMemberRepository.findByClubIdAndId(clubId, memberId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.MEMBER_NOT_FOUND));

        if (member.getActivityStatus() != ActivityStatus.GUEST) {
            throw new BusinessException(ClubErrorCode.NOT_GUEST_STATUS);
        }

        if (param.activityEndDate() == null) {
            throw new BusinessException(ClubErrorCode.ACTIVITY_END_DATE_REQUIRED);
        }

        ClubGeneration generation = clubGenerationRepository.findById(param.generationId())
                .orElseThrow(() -> new BusinessException(ClubErrorCode.GENERATION_NOT_FOUND));

        member.updateActivityStatus(ActivityStatus.ACTIVE);
        member.setJoinGeneration(generation);
        member.setActivityEndDate(param.activityEndDate());

        log.info("동아리 GUEST→MEMBER 승격: clubId={}, memberId={}, generationId={}",
                clubId, memberId, param.generationId());
    }

    @Transactional
    public void requestKick(Long userId, Long clubId, Long targetMemberId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        validateClubPermission(clubId, userId, Permission.MANAGE_MEMBER);

        ClubMember targetMember = clubMemberRepository.findByClubIdAndId(clubId, targetMemberId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.MEMBER_NOT_FOUND));

        if (clubKickVoteRepository.existsByTargetMember(targetMember)) {
            throw new BusinessException(ClubErrorCode.KICK_ALREADY_IN_PROGRESS);
        }

        List<ClubMember> permissionMembers = clubMemberRepository
                .findByClubIdAndStatusAndActivityStatus(clubId, BaseStatus.ACTIVE, ActivityStatus.ACTIVE)
                .stream()
                .filter(m -> m.getRole().hasPermission(Permission.MANAGE_MEMBER))
                .toList();

        for (ClubMember voter : permissionMembers) {
            boolean isRequester = voter.getUser().getId().equals(userId);
            clubKickVoteRepository.save(
                    ClubKickVote.builder()
                            .club(club)
                            .targetMember(targetMember)
                            .voter(voter.getUser())
                            .isApproved(isRequester ? true : null)
                            .build()
            );
        }

        if (permissionMembers.size() == 1) {
            targetMember.updateActivityStatus(ActivityStatus.KICKEDOUT);
            clubKickVoteRepository.deleteByTargetMember(targetMember);
            log.info("동아리 회원 내보내기 즉시 승인 (권한자 1명): clubId={}, targetMemberId={}",
                    clubId, targetMemberId);
        } else {
            log.info("동아리 회원 내보내기 투표 시작: clubId={}, targetMemberId={}, 투표 대상={}명",
                    clubId, targetMemberId, permissionMembers.size());
        }
    }

    @Transactional
    public void voteKick(Long userId, Long clubId, Long targetMemberId, boolean approved) {
        clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        ClubMember targetMember = clubMemberRepository.findByClubIdAndId(clubId, targetMemberId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.MEMBER_NOT_FOUND));

        if (!clubKickVoteRepository.existsByTargetMember(targetMember)) {
            throw new BusinessException(ClubErrorCode.KICK_NOT_IN_PROGRESS);
        }

        ClubKickVote vote = clubKickVoteRepository.findByTargetMemberAndVoterId(targetMember, userId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.KICK_VOTE_NOT_FOUND));

        if (!approved) {
            clubKickVoteRepository.deleteByTargetMember(targetMember);
            log.info("동아리 내보내기 투표 거부 → 종료: clubId={}, targetMemberId={}", clubId, targetMemberId);
            return;
        }

        vote.approve();

        List<ClubKickVote> allVotes = clubKickVoteRepository.findByTargetMember(targetMember);
        boolean allApproved = allVotes.stream().allMatch(v -> Boolean.TRUE.equals(v.getIsApproved()));

        if (allApproved) {
            targetMember.updateActivityStatus(ActivityStatus.KICKEDOUT);
            clubKickVoteRepository.deleteByTargetMember(targetMember);
            log.info("동아리 내보내기 전원 동의 → 완료: clubId={}, targetMemberId={}", clubId, targetMemberId);
        }
    }

    @Transactional(readOnly = true)
    public List<ClubMemberResult> getMembers(Long userId, Long clubId, ActivityStatus activityStatus) {
        clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.NOT_CLUB_MEMBER));

        List<ClubMember> members;
        if (activityStatus != null) {
            members = clubMemberRepository.findByClubIdAndActivityStatus(clubId, activityStatus);
        } else {
            members = clubMemberRepository.findByClubId(clubId);
        }

        return members.stream()
                .map(ClubMemberResult::from)
                .toList();
    }

    @Transactional
    public void updateClub(Long userId, Long clubId, UpdateClubParam param) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        validateClubPermission(clubId, userId, Permission.EDIT_INFO);

        if (param.categoryIds() == null || param.categoryIds().isEmpty()) {
            throw new BusinessException(ClubErrorCode.CLUB_CATEGORY_REQUIRED);
        }
        if (param.categoryIds().size() > 3) {
            throw new BusinessException(ClubErrorCode.CLUB_CATEGORY_LIMIT_EXCEEDED);
        }
        if (param.regionIds() == null || param.regionIds().isEmpty()) {
            throw new BusinessException(ClubErrorCode.CLUB_REGION_REQUIRED);
        }

        club.update(param.name(), param.description(), param.coverImg(),
                param.activityType(), param.foundedDate());

        clubCategoryRepository.deleteByClubId(clubId);
        saveClubCategories(club, param.categoryIds());

        clubRegionRepository.deleteByClubId(clubId);
        saveClubRegions(club, param.regionIds());

        log.info("동아리 정보 수정: clubId={}, userId={}", clubId, userId);
    }

    @Transactional
    public void updateClubSettings(Long userId, Long clubId, UpdateClubSettingsParam param) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        validateClubPermission(clubId, userId, Permission.EDIT_INFO);

        if (clubRecruitmentPort.hasActiveRecruitment(clubId)) {
            throw new BusinessException(ClubErrorCode.ACTIVE_RECRUITMENT_EXISTS);
        }

        Boolean isVerificationRequired = param.isVerificationRequired();
        Gender gender = param.gender();
        Long minAge = param.minAge();
        Long maxAge = param.maxAge();

        if (!Boolean.TRUE.equals(isVerificationRequired)) {
            gender = Gender.ANY;
            minAge = null;
            maxAge = null;
        } else {
            if (gender == null) {
                gender = Gender.ANY;
            }
        }

        if (!Boolean.TRUE.equals(isVerificationRequired)
                && (param.gender() != null && param.gender() != Gender.ANY
                    || param.minAge() != null || param.maxAge() != null)) {
            throw new BusinessException(ClubErrorCode.VERIFICATION_REQUIRED_FOR_TAG);
        }

        club.updateSettings(isVerificationRequired, gender, minAge, maxAge);
        log.info("동아리 설정 변경: clubId={}, userId={}, isVerificationRequired={}",
                clubId, userId, isVerificationRequired);
    }

    @Transactional
    public void requestDeletion(Long userId, Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        validateClubPermission(clubId, userId, Permission.DELETE_CLUB);

        if (club.getDeletionStatus() != ClubDeletionStatus.NONE) {
            throw new BusinessException(ClubErrorCode.DELETION_ALREADY_IN_PROGRESS);
        }

        List<ClubMember> deletePermissionMembers = clubMemberRepository
                .findByClubIdAndStatusAndActivityStatus(clubId, BaseStatus.ACTIVE, ActivityStatus.ACTIVE)
                .stream()
                .filter(m -> m.getRole().hasPermission(Permission.DELETE_CLUB))
                .toList();

        club.startDeletionVoting();

        for (ClubMember member : deletePermissionMembers) {
            boolean isRequester = member.getUser().getId().equals(userId);
            clubDeletionVoteRepository.save(
                    ClubDeletionVote.builder()
                            .club(club)
                            .user(member.getUser())
                            .isApproved(isRequester ? true : null)
                            .build()
            );
        }

        if (deletePermissionMembers.size() == 1) {
            club.approveDeletion();
            log.info("동아리 삭제 즉시 승인 (권한자 1명): clubId={}", clubId);
        } else {
            log.info("동아리 삭제 투표 시작: clubId={}, 투표 대상={}명", clubId, deletePermissionMembers.size());
        }
    }

    @Transactional
    public void voteDeletion(Long userId, Long clubId, boolean approved) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        if (club.getDeletionStatus() != ClubDeletionStatus.VOTING) {
            throw new BusinessException(ClubErrorCode.DELETION_NOT_VOTING);
        }

        ClubDeletionVote vote = clubDeletionVoteRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.DELETION_VOTE_NOT_FOUND));

        if (!approved) {
            club.cancelDeletionVoting();
            clubDeletionVoteRepository.deleteByClubId(clubId);
            log.info("동아리 삭제 투표 거부 → 투표 종료: clubId={}, userId={}", clubId, userId);
            return;
        }

        vote.approve();

        List<ClubDeletionVote> allVotes = clubDeletionVoteRepository.findByClubId(clubId);
        boolean allApproved = allVotes.stream().allMatch(v -> Boolean.TRUE.equals(v.getIsApproved()));

        if (allApproved) {
            club.approveDeletion();
            log.info("동아리 삭제 전원 동의 → 유예 기간 시작: clubId={}", clubId);
        }
    }

    @Transactional
    public void cancelDeletion(Long userId, Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        if (club.getDeletionStatus() != ClubDeletionStatus.APPROVED) {
            throw new BusinessException(ClubErrorCode.DELETION_NOT_APPROVED);
        }

        if (club.getIsSanctionDeletion()) {
            throw new BusinessException(ClubErrorCode.DELETION_SANCTION_NOT_CANCELABLE);
        }

        ClubMember member = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.NOT_CLUB_MEMBER));

        if (!PRESIDENT_ROLE_NAME.equals(member.getRole().getName()) || !member.getRole().getIsDefault()) {
            throw new BusinessException(ClubErrorCode.DELETION_CANCEL_FORBIDDEN);
        }

        if (club.getScheduledDeleteAt() != null
                && LocalDateTime.now().isAfter(club.getScheduledDeleteAt())) {
            throw new BusinessException(ClubErrorCode.DELETION_GRACE_PERIOD_EXPIRED);
        }

        club.cancelDeletion();
        clubDeletionVoteRepository.deleteByClubId(clubId);
        log.info("동아리 삭제 취소: clubId={}, userId={}", clubId, userId);
    }

    @Transactional
    public int processGraduation() {
        LocalDate today = LocalDate.now();
        List<ClubMember> targets = clubMemberRepository
                .findByActivityStatusAndActivityEndDateLessThanEqual(ActivityStatus.ACTIVE, today);

        for (ClubMember member : targets) {
            member.updateActivityStatus(ActivityStatus.GRADUATED);
        }

        if (!targets.isEmpty()) {
            log.info("동아리 자동 수료 처리 완료: {}명", targets.size());
        }
        return targets.size();
    }

    private void validateClubPermission(Long clubId, Long userId, Permission permission) {
        ClubMember member = clubMemberRepository
                .findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.NOT_CLUB_MEMBER));

        if (!member.getRole().hasPermission(permission)) {
            throw new BusinessException(ClubErrorCode.CLUB_PERMISSION_DENIED);
        }
    }

    @Transactional(readOnly = true)
    public Slice<ClubSummaryResult> getClubList(GetClubListParam param) {
        return clubRepository.findClubList(param);
    }

    private void validateCreateClub(Long userId, CreateClubParam param) {
        if (param.categoryIds() == null || param.categoryIds().isEmpty()) {
            throw new BusinessException(ClubErrorCode.CLUB_CATEGORY_REQUIRED);
        }
        if (param.categoryIds().size() > 3) {
            throw new BusinessException(ClubErrorCode.CLUB_CATEGORY_LIMIT_EXCEEDED);
        }
        if (param.regionIds() == null || param.regionIds().isEmpty()) {
            throw new BusinessException(ClubErrorCode.CLUB_REGION_REQUIRED);
        }

        long presidentCount = clubMemberRepository
                .findByUserIdAndStatusAndActivityStatus(userId, BaseStatus.ACTIVE, ActivityStatus.ACTIVE)
                .stream()
                .filter(m -> PRESIDENT_ROLE_NAME.equals(m.getRole().getName()) && m.getRole().getIsDefault())
                .count();

        if (presidentCount >= MAX_PRESIDENT_CLUBS) {
            throw new BusinessException(ClubErrorCode.CLUB_PRESIDENT_LIMIT_EXCEEDED);
        }
    }

    private void saveClubCategories(Club club, List<Long> categoryIds) {
        for (Long categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.CATEGORY_NOT_FOUND));
            clubCategoryRepository.save(ClubCategory.builder().club(club).category(category).build());
        }
    }

    private void saveClubRegions(Club club, List<Long> regionIds) {
        for (Long regionId : regionIds) {
            Region region = regionRepository.findById(regionId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.REGION_NOT_FOUND));
            clubRegionRepository.save(ClubRegion.builder().club(club).region(region).build());
        }
    }

    private void initializeDefaultRoles(Club club) {
        ClubRole president = ClubRole.builder()
                .club(club).name("회장").permissions(Permission.all())
                .isStaff(true).isDefault(true).build();

        ClubRole vicePresident = ClubRole.builder()
                .club(club).name("부회장")
                .permissions(Permission.combine(
                        Permission.PROPOSE_ACTIVITY, Permission.MANAGE_ACTIVITY,
                        Permission.EDIT_INFO, Permission.NETWORK_CHAT,
                        Permission.ANSWER_INQUIRY, Permission.MANAGE_RECRUITMENT,
                        Permission.DECIDE_ADMISSION, Permission.VIEW_APPLICATION,
                        Permission.MANAGE_MEMBER, Permission.MANAGE_FEDERATION,
                        Permission.MANAGE_NOTICE, Permission.MANAGE_FEED,
                        Permission.MANAGE_ATTENDANCE, Permission.MANAGE_CALENDAR,
                        Permission.MANAGE_STORAGE
                ))
                .isStaff(true).isDefault(true).build();

        ClubRole hr = ClubRole.builder()
                .club(club).name("인사")
                .permissions(Permission.combine(
                        Permission.MANAGE_RECRUITMENT, Permission.DECIDE_ADMISSION, Permission.VIEW_APPLICATION))
                .isStaff(true).isDefault(true).build();

        ClubRole pr = ClubRole.builder()
                .club(club).name("홍보")
                .permissions(Permission.combine(
                        Permission.MANAGE_ACTIVITY, Permission.ANSWER_INQUIRY, Permission.NETWORK_CHAT))
                .isStaff(true).isDefault(true).build();

        ClubRole member = ClubRole.builder()
                .club(club).name("부원").permissions(0L)
                .isStaff(false).isDefault(true).build();

        clubRoleRepository.saveAll(List.of(president, vicePresident, hr, pr, member));
    }
}
