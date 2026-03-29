package com.dewple.club.service;

import com.dewple.club.entity.ClubMember;
import com.dewple.club.entity.ClubRole;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.repository.ClubMemberRepository;
import com.dewple.club.repository.ClubRoleRepository;
import com.dewple.common.entity.Club;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import com.dewple.club.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubRoleService {

    private final ClubRepository clubRepository;
    private final ClubRoleRepository clubRoleRepository;
    private final ClubMemberRepository clubMemberRepository;

    private static final String PRESIDENT_ROLE_NAME = "회장";

    @Transactional
    public ClubRoleResult createRole(Long userId, Long clubId, CreateClubRoleParam param) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        validateRoleManagePermission(clubId, userId);

        if (clubRoleRepository.existsByClubIdAndName(clubId, param.name())) {
            throw new BusinessException(ClubErrorCode.ROLE_NAME_DUPLICATED);
        }

        long permissionBits = convertPermissions(param.permissions());

        ClubRole role = ClubRole.builder()
                .club(club)
                .name(param.name())
                .permissions(permissionBits)
                .isStaff(param.isStaff())
                .isDefault(false)
                .build();

        clubRoleRepository.save(role);
        log.info("동아리 역할 생성: clubId={}, roleId={}, name={}", clubId, role.getId(), param.name());
        return ClubRoleResult.from(role);
    }

    @Transactional
    public ClubRoleResult updateRole(Long userId, Long clubId, Long roleId, UpdateClubRoleParam param) {
        clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

        ClubRole role = clubRoleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.ROLE_NOT_FOUND));

        if (PRESIDENT_ROLE_NAME.equals(role.getName()) && role.getIsDefault()) {
            throw new BusinessException(ClubErrorCode.PRESIDENT_ROLE_NOT_MODIFIABLE);
        }

        if (role.getIsDefault()) {
            validatePresident(clubId, userId);
        } else {
            validateRoleManagePermission(clubId, userId);
        }

        if (!role.getName().equals(param.name())
                && clubRoleRepository.existsByClubIdAndName(clubId, param.name())) {
            throw new BusinessException(ClubErrorCode.ROLE_NAME_DUPLICATED);
        }

        long permissionBits = convertPermissions(param.permissions());
        role.update(param.name(), permissionBits, param.isStaff());

        log.info("동아리 역할 수정: clubId={}, roleId={}", clubId, roleId);
        return ClubRoleResult.from(role);
    }

    @Transactional(readOnly = true)
    public List<ClubRoleResult> getRoles(Long clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new BusinessException(ClubErrorCode.CLUB_NOT_FOUND);
        }

        return clubRoleRepository.findByClubId(clubId).stream()
                .map(ClubRoleResult::from)
                .toList();
    }

    private void validateRoleManagePermission(Long clubId, Long userId) {
        ClubMember member = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.NOT_CLUB_MEMBER));

        ClubRole role = member.getRole();
        boolean isPresident = PRESIDENT_ROLE_NAME.equals(role.getName()) && role.getIsDefault();

        if (!isPresident && !role.hasPermission(Permission.MANAGE_MEMBER)) {
            throw new BusinessException(ClubErrorCode.ROLE_MANAGE_FORBIDDEN);
        }
    }

    private void validatePresident(Long clubId, Long userId) {
        ClubMember member = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.NOT_CLUB_MEMBER));

        ClubRole role = member.getRole();
        if (!PRESIDENT_ROLE_NAME.equals(role.getName()) || !role.getIsDefault()) {
            throw new BusinessException(ClubErrorCode.DEFAULT_ROLE_MODIFY_FORBIDDEN);
        }
    }

    private long convertPermissions(List<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return 0L;
        }
        long bits = 0L;
        for (String name : permissionNames) {
            bits |= Permission.valueOf(name).getValue();
        }
        return bits;
    }
}
