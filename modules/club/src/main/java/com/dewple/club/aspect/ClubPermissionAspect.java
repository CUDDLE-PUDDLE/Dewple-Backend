package com.dewple.club.aspect;

import com.dewple.club.annotation.RequireClubPermission;
import com.dewple.club.entity.ClubMember;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.repository.ClubMemberRepository;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ClubPermissionAspect {

    private final ClubMemberRepository clubMemberRepository;

    @Before("@annotation(requireClubPermission)")
    public void checkPermission(JoinPoint joinPoint, RequireClubPermission requireClubPermission) {
        Object[] args = joinPoint.getArgs();
        Long clubId = (Long) args[0];
        Long userId = (Long) args[1];

        Permission required = requireClubPermission.value();

        ClubMember member = clubMemberRepository
                .findByClubIdAndUserIdAndActivityStatus(clubId, userId, ActivityStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ClubErrorCode.NOT_CLUB_MEMBER));

        if (member.getRole() == null || !member.getRole().hasPermission(required)) {
            throw new BusinessException(ClubErrorCode.INSUFFICIENT_PERMISSION);
        }
    }
}
