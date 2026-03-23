package com.dewple.app_worker.infra;

import com.dewple.club.entity.ClubMember;
import com.dewple.club.repository.ClubMemberRepository;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.user.port.WithdrawalClubPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalClubAdapter implements WithdrawalClubPort {

    private final ClubMemberRepository clubMemberRepository;

    @Override
    public void transferPresidentRoles(Long userId) {
        // worker에서는 하드삭제 시점에만 호출되므로 승계는 불필요 (소프트삭제 시점에 이미 처리됨)
    }

    @Override
    public void removeFromAllClubs(Long userId) {
        List<ClubMember> memberships = clubMemberRepository.findByUserIdAndStatusAndActivityStatus(
                userId, BaseStatus.ACTIVE, ActivityStatus.ACTIVE);

        for (ClubMember membership : memberships) {
            membership.updateActivityStatus(ActivityStatus.LEFT);
            membership.inactivate();
            log.info("하드삭제로 인한 동아리 탈퇴: userId={}, clubId={}",
                    userId, membership.getClub().getId());
        }
    }
}
