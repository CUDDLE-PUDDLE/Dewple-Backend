package com.dewple.club.service;

import com.dewple.club.entity.*;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.repository.*;
import com.dewple.common.entity.Category;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.Region;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.BaseStatus;
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
                .club(club).name("부회장").permissions(0L)
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
