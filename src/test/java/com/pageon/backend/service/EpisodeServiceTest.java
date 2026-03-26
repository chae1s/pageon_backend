package com.pageon.backend.service;

import com.pageon.backend.dto.request.EpisodeRatingRequest;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.service.provider.EpisodeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;


@ActiveProfiles("test")
@DisplayName("EpisodeService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class EpisodeServiceTest {
    @InjectMocks
    private EpisodeService episodeService;
    @Mock
    private List<EpisodeProvider> providers;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IdempotentService idempotentService;
    @Mock
    private EpisodeProvider episodeProvider;

    @BeforeEach
    void setUp() {
        lenient().when(providers.stream()).thenReturn(Stream.of(episodeProvider));
        lenient().when(episodeProvider.supports(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("에피소드 상세 조회 성공")
    void getEpisodeDetail_withValidInput_shouldReturnDetail() {
        // given
        Object detail = mock(Object.class);
        when(episodeProvider.findEpisodeDetail(1L, 1L)).thenReturn(detail);

        // when
        Object result = episodeService.getEpisodeDetail(1L, "webnovels", 1L);

        // then
        assertNotNull(result);
        verify(episodeProvider).findEpisodeDetail(1L, 1L);
    }

    @Test
    @DisplayName("지원하지 않는 contentType이면 CustomException 발생")
    void getProvider_withInvalidContentType_shouldThrowException() {
        // given
        when(episodeProvider.supports(anyString())).thenReturn(false);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> episodeService.getEpisodeDetail(1L, "invalid", 1L));

        assertEquals(ErrorCode.INVALID_CONTENT_TYPE, ErrorCode.valueOf(exception.getErrorCode()));
    }

    @Test
    @DisplayName("에피소드 평점 등록 성공")
    void rateEpisode_withValidInput_shouldRateEpisode() {
        // given
        User user = mock(User.class);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        doNothing().when(idempotentService).isValidIdempotent(any());

        EpisodeRatingRequest request = new EpisodeRatingRequest(5);

        // when
        episodeService.rateEpisode(1L, "webnovels", 1L, request);

        // then
        verify(episodeProvider).rateEpisode(eq(user), eq(1L), eq(5));
    }

    @Test
    @DisplayName("중복 평점 요청 시 CustomException 발생")
    void rateEpisode_withDuplicateRequest_shouldThrowException() {
        // given
        doThrow(new CustomException(ErrorCode.DUPLICATION_REQUEST))
                .when(idempotentService).isValidIdempotent(any());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> episodeService.rateEpisode(1L, "webnovels", 1L,
                        new EpisodeRatingRequest(5)));

        assertEquals(ErrorCode.DUPLICATION_REQUEST, ErrorCode.valueOf(exception.getErrorCode()));
        verify(episodeProvider, never()).rateEpisode(any(), any(), any());
    }

    @Test
    @DisplayName("에피소드 평점 수정 성공")
    void updateEpisodeRating_withValidInput_shouldUpdateRating() {
        // given
        EpisodeRatingRequest request = new EpisodeRatingRequest(3);

        // when
        episodeService.updateEpisodeRating(1L, "webnovels", 1L, request);

        // then
        verify(episodeProvider).updateEpisodeRating(eq(1L), eq(1L), eq(3));
    }


}