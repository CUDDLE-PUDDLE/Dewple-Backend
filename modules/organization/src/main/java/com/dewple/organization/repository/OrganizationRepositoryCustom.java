package com.dewple.organization.repository;

import com.dewple.organization.service.GetOrganizationListParam;
import com.dewple.organization.service.OrganizationSummaryResult;
import org.springframework.data.domain.Slice;

public interface OrganizationRepositoryCustom {

    Slice<OrganizationSummaryResult> findOrganizationList(GetOrganizationListParam param);
}
