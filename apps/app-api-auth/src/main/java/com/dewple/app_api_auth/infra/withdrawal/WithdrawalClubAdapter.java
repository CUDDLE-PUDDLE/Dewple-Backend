package com.dewple.app_api_auth.infra.withdrawal;

import com.dewple.club.entity.ClubMember;
import com.dewple.club.entity.ClubRole;
import com.dewple.club.repository.ClubMemberRepository;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.Permission;
import com.dewple.user.port.WithdrawalClubPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalClubAdapter implements WithdrawalClubPort {

    private final ClubMemberRepository clubMemberRepository;

    @Override
    public void transferPresidentRoles(Long userId) {
        List<ClubMember> memberships = clubMemberRepository.findByUserIdAndStatusAndActivityStatus(
                userId, BaseStatus.ACTIVE, ActivityStatus.ACTIVE);

        for (ClubMember membership : memberships) {
            ClubRole role = membership.getRole();
            if (role == null || !role.hasPermission(Permission.DELEGATE_PRESIDENT)) {
                continue;
            }

            Long clubId = membership.getClub().getId();

            List<ClubMember> candidates = clubMemberRepository
                    .findByClubIdAndStatusAndActivityStatusAndUserIdNot(
                            clubId, BaseStatus.ACTIVE, ActivityStatus.ACTIVE, userId);

            if (candidates.isEmpty()) {
                // TODO: 동아리 멤버가 아무도 없으면 동아리 처리 정책 필요 (삭제? 유지?)
                log.warn("동아리 승계 대상 없음: clubId={}", clubId);
                membership.changeRole(null);
                continue;
            }

            // 승계 우선순위: 운영진(isStaff) > 권한 수 많은 순 > 일반 부원
            ClubMember successor = candidates.stream()
                    .sorted(Comparator
                            .comparing((ClubMember m) -> m.getRole() != null && m.getRole().getIsStaff())
                            .reversed()
                            .thenComparing(Comparator
                                    .comparingLong((ClubMember m) ->
                                            m.getRole() != null ? Long.bitCount(m.getRole().getPermissions()) : 0)
                                    .reversed()))
                    .findFirst()
                    .orElse(candidates.get(0));

            successor.changeRole(role);
            membership.changeRole(null);

            log.info("동아리 회장 승계: clubId={}, newPresidentUserId={}",
                    clubId, successor.getUser().getId());
        }
    }
}
