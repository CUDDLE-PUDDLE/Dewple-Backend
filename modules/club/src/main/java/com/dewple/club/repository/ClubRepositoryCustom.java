package com.dewple.club.repository;

import com.dewple.club.service.ClubSummaryResult;
import com.dewple.club.service.GetClubListParam;
import org.springframework.data.domain.Slice;

public interface ClubRepositoryCustom {

    Slice<ClubSummaryResult> findClubList(GetClubListParam param);
}
