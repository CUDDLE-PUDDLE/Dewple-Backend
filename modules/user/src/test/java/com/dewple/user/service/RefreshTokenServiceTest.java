package com.dewple.user.service;

import com.dewple.common.entity.User;
import com.dewple.user.entity.RefreshToken;
import com.dewple.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final String TEST_TOKEN = "test-refresh-token";
    private static final long TEST_EXPIRY_SECONDS = 604800L;

    @Nested
    @DisplayName("save - 리프레시 토큰 저장")
    class Save {

        @Test
        @DisplayName("성공: 리프레시 토큰 저장")
        void success() {
            // given
            User user = createUser();
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            RefreshToken result = refreshTokenService.save(user, TEST_TOKEN, TEST_EXPIRY_SECONDS);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo(TEST_TOKEN);
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getExpireAt()).isNotNull();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getToken()).isEqualTo(TEST_TOKEN);
        }
    }

    @Nested
    @DisplayName("findByToken - 리프레시 토큰 조회")
    class FindByToken {

        @Test
        @DisplayName("성공: 존재하는 토큰 조회")
        void success() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .user(createUser())
                    .token(TEST_TOKEN)
                    .build();
            given(refreshTokenRepository.findByToken(TEST_TOKEN))
                    .willReturn(Optional.of(refreshToken));

            // when
            Optional<RefreshToken> result = refreshTokenService.findByToken(TEST_TOKEN);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getToken()).isEqualTo(TEST_TOKEN);
        }

        @Test
        @DisplayName("성공: 존재하지 않는 토큰 조회 시 빈 Optional 반환")
        void notFound() {
            // given
            given(refreshTokenRepository.findByToken("non-existent"))
                    .willReturn(Optional.empty());

            // when
            Optional<RefreshToken> result = refreshTokenService.findByToken("non-existent");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByToken - 리프레시 토큰 단건 삭제")
    class DeleteByToken {

        @Test
        @DisplayName("성공: 토큰 삭제")
        void success() {
            // when
            refreshTokenService.deleteByToken(TEST_TOKEN);

            // then
            verify(refreshTokenRepository).deleteByToken(TEST_TOKEN);
        }
    }

    @Nested
    @DisplayName("deleteAllByUserId - 유저의 전체 리프레시 토큰 삭제")
    class DeleteAllByUserId {

        @Test
        @DisplayName("성공: 유저의 전체 토큰 삭제")
        void success() {
            // when
            refreshTokenService.deleteAllByUserId(1L);

            // then
            verify(refreshTokenRepository).deleteAllByUserId(1L);
        }
    }

    private User createUser() {
        return User.builder()
                .userId("dewple123")
                .password("encoded-password")
                .name("홍길동")
                .phone("01012345678")
                .build();
    }
}
