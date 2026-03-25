package com.dewple.organization.service;

import com.dewple.common.entity.User;
import com.dewple.common.enums.*;
import com.dewple.common.exception.BusinessException;
import com.dewple.organization.entity.Organization;
import com.dewple.organization.exception.OrganizationErrorCode;
import com.dewple.organization.repository.OrganizationRepository;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private static final Long USER_ID = 1L;

    private User createUser() {
        User user = User.builder()
                .userId("testuser")
                .name("테스트")
                .phone("01012345678")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private CreateOrganizationParam createValidParam() {
        return new CreateOrganizationParam(
                "서울대 동아리 연합회",
                "서울대학교 동아리들을 관리합니다.",
                OrganizationType.UNIVERSITY,
                ActivityType.BOTH,
                "contact@snu.ac.kr",
                "01012345678",
                ContactPreference.EMAIL,
                null,
                List.of(1L, 2L),
                List.of(1L)
        );
    }

    private Organization createPendingOrganization() {
        Organization org = Organization.builder()
                .creator(createUser())
                .name("테스트 연합회")
                .purpose("목적")
                .type(OrganizationType.UNIVERSITY)
                .activityType(ActivityType.BOTH)
                .contactEmail("a@b.com")
                .contactPhone("010")
                .contactPreference(ContactPreference.EMAIL)
                .categoryIds("[1]")
                .regionIds("[1]")
                .build();
        ReflectionTestUtils.setField(org, "id", 100L);
        return org;
    }

    @Nested
    @DisplayName("apply - 연합회 생성 신청")
    class Apply {

        @Test
        @DisplayName("성공: 유효한 정보로 신청")
        void success() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));
            given(organizationRepository.save(any(Organization.class)))
                    .willAnswer(invocation -> {
                        Organization org = invocation.getArgument(0);
                        ReflectionTestUtils.setField(org, "id", 100L);
                        return org;
                    });

            // when
            Organization result = organizationService.apply(USER_ID, createValidParam());

            // then
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getName()).isEqualTo("서울대 동아리 연합회");
            assertThat(result.getType()).isEqualTo(OrganizationType.UNIVERSITY);
            assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저")
        void failUserNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.apply(999L, createValidParam()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("실패: 카테고리 미선택")
        void failNoCategoryIds() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            CreateOrganizationParam param = new CreateOrganizationParam(
                    "테스트", "목적", OrganizationType.OTHER, ActivityType.BOTH,
                    "a@b.com", "010", ContactPreference.EMAIL, null,
                    List.of(), List.of(1L));

            assertThatThrownBy(() -> organizationService.apply(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.CATEGORY_REQUIRED));
        }

        @Test
        @DisplayName("실패: 카테고리 4개 초과")
        void failTooManyCategories() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            CreateOrganizationParam param = new CreateOrganizationParam(
                    "테스트", "목적", OrganizationType.OTHER, ActivityType.BOTH,
                    "a@b.com", "010", ContactPreference.EMAIL, null,
                    List.of(1L, 2L, 3L, 4L), List.of(1L));

            assertThatThrownBy(() -> organizationService.apply(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.CATEGORY_LIMIT_EXCEEDED));
        }

        @Test
        @DisplayName("실패: 지역 미선택")
        void failNoRegionIds() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            CreateOrganizationParam param = new CreateOrganizationParam(
                    "테스트", "목적", OrganizationType.OTHER, ActivityType.BOTH,
                    "a@b.com", "010", ContactPreference.EMAIL, null,
                    List.of(1L), List.of());

            assertThatThrownBy(() -> organizationService.apply(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.REGION_REQUIRED));
        }
    }

    @Nested
    @DisplayName("getOrganizationList - 연합회 목록 조회")
    class GetOrganizationList {

        @Test
        @DisplayName("성공: 필터 없이 목록 조회")
        void successWithoutFilters() {
            // given
            List<OrganizationSummaryResult> content = List.of(
                    new OrganizationSummaryResult(1L, "서울대 연합회", null,
                            OrganizationType.UNIVERSITY, ActivityType.BOTH, "[1, 2]", "[1]"),
                    new OrganizationSummaryResult(2L, "기업 연합회", "cover.jpg",
                            OrganizationType.ENTERPRISE, ActivityType.OFFLINE, "[3]", "[2]")
            );
            Slice<OrganizationSummaryResult> slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);

            GetOrganizationListParam param = new GetOrganizationListParam(
                    null, null, null, null, PageRequest.of(0, 10)
            );
            given(organizationRepository.findOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            // when
            Slice<OrganizationSummaryResult> result = organizationService.getOrganizationList(param);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).name()).isEqualTo("서울대 연합회");
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("성공: 종류 필터로 조회")
        void successWithTypeFilter() {
            // given
            List<OrganizationSummaryResult> content = List.of(
                    new OrganizationSummaryResult(1L, "서울대 연합회", null,
                            OrganizationType.UNIVERSITY, ActivityType.BOTH, "[1]", "[1]")
            );
            Slice<OrganizationSummaryResult> slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);

            GetOrganizationListParam param = new GetOrganizationListParam(
                    null, null, null, OrganizationType.UNIVERSITY, PageRequest.of(0, 10)
            );
            given(organizationRepository.findOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            // when
            Slice<OrganizationSummaryResult> result = organizationService.getOrganizationList(param);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).type()).isEqualTo(OrganizationType.UNIVERSITY);
        }

        @Test
        @DisplayName("성공: 빈 결과")
        void successEmptyResult() {
            // given
            Slice<OrganizationSummaryResult> slice = new SliceImpl<>(
                    List.of(), PageRequest.of(0, 10), false
            );

            GetOrganizationListParam param = new GetOrganizationListParam(
                    999L, null, null, null, PageRequest.of(0, 10)
            );
            given(organizationRepository.findOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            // when
            Slice<OrganizationSummaryResult> result = organizationService.getOrganizationList(param);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("approve - 연합회 생성 승인")
    class Approve {

        @Test
        @DisplayName("성공: 대기 상태 연합회 승인")
        void success() {
            Organization org = createPendingOrganization();
            given(organizationRepository.findById(100L)).willReturn(Optional.of(org));

            organizationService.approve(100L);

            assertThat(org.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        }

        @Test
        @DisplayName("실패: 이미 승인된 연합회")
        void failAlreadyApproved() {
            Organization org = createPendingOrganization();
            org.approve();
            given(organizationRepository.findById(100L)).willReturn(Optional.of(org));

            assertThatThrownBy(() -> organizationService.approve(100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_PENDING));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 연합회")
        void failNotFound() {
            given(organizationRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.approve(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("reject - 연합회 생성 반려")
    class Reject {

        @Test
        @DisplayName("성공: 반려 사유와 함께 반려")
        void success() {
            Organization org = createPendingOrganization();
            given(organizationRepository.findById(100L)).willReturn(Optional.of(org));

            organizationService.reject(100L, "관리 대상 동아리 정보가 불충분합니다.");

            assertThat(org.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
            assertThat(org.getRejectionReason()).isEqualTo("관리 대상 동아리 정보가 불충분합니다.");
        }

        @Test
        @DisplayName("실패: 이미 반려된 연합회")
        void failAlreadyRejected() {
            Organization org = createPendingOrganization();
            org.reject("이전 사유");
            given(organizationRepository.findById(100L)).willReturn(Optional.of(org));

            assertThatThrownBy(() -> organizationService.reject(100L, "다른 사유"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_PENDING));
        }
    }
}
