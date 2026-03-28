package com.dewple.app_api_auth.infra.club;

import com.dewple.club.port.ClubRecruitmentPort;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.recruitment.repository.RecruitmentPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClubRecruitmentAdapter implements ClubRecruitmentPort {

    private final RecruitmentPostingRepository recruitmentPostingRepository;

    @Override
    public boolean hasActiveRecruitment(Long clubId) {
        return !recruitmentPostingRepository.findPostingsForPublicList(
                clubId, List.of(RecruitmentStatus.OPEN), BaseStatus.ACTIVE
        ).isEmpty();
    }
}
