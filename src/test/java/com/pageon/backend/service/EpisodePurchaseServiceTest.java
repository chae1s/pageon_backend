package com.pageon.backend.service;

import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.entity.*;
import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import com.pageon.backend.service.provider.EpisodeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DisplayName("EpisodePurchaseService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class EpisodePurchaseServiceTest {
    @InjectMocks
    private EpisodePurchaseService episodePurchaseService;

    @Mock
    private List<EpisodeProvider> providers;
    @Mock
    private EpisodeProvider episodeProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EpisodePurchaseRepository episodePurchaseRepository;
    @Mock
    private PointTransactionService pointTransactionService;
    @Mock
    private IdempotentService idempotentService;
    @Mock
    private ActionLogService actionLogService;


    @BeforeEach
    void setUp() {
        lenient().when(providers.stream()).thenReturn(Stream.of(episodeProvider));
        lenient().when(episodeProvider.supports(anyString())).thenReturn(true);
    }

    private User mockUser(int pointBalance) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(1L);
        when(user.getPointBalance()).thenReturn(pointBalance);
        return user;
    }

    private EpisodePurchaseService.EpisodeInfo mockEpisodeInfo() {
        EpisodeBase episode = mock(EpisodeBase.class);
        Content content = mock(Content.class);

        lenient().when(content.getId()).thenReturn(1L);
        lenient().when(content.getTitle()).thenReturn("제목");
        lenient().when(episode.getEpisodeNum()).thenReturn(1);
        return new EpisodePurchaseService.EpisodeInfo(content, 100, episode);
    }

    private void givenCommonSetup(User user, EpisodePurchaseService.EpisodeInfo episodeInfo) {
        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(episodeProvider.getEpisodeInfo(any(), any())).thenReturn(episodeInfo);
    }

    @Test
    @DisplayName("에피소드 소장 성공")
    void shouldSavePurchaseRecord_whenUserBuysEpisode() {
        // given
        User user = mockUser(1000);
        EpisodePurchaseService.EpisodeInfo episodeInfo = mockEpisodeInfo();
        givenCommonSetup(user, episodeInfo);

        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(any(), any(), any()))
                .thenReturn(Optional.empty());

        EpisodePurchase savedPurchase = mock(EpisodePurchase.class);
        when(savedPurchase.getId()).thenReturn(1L);
        when(episodePurchaseRepository.save(any())).thenReturn(savedPurchase);

        ArgumentCaptor<EpisodePurchase> captor = ArgumentCaptor.forClass(EpisodePurchase.class);

        // when
        episodePurchaseService.createPurchaseHistory(1L, "webnovels", 100L, PurchaseType.OWN);

        // then
        verify(episodePurchaseRepository).save(captor.capture());
        assertEquals(PurchaseType.OWN, captor.getValue().getPurchaseType());
        assertNull(captor.getValue().getExpiredAt());
        verify(pointTransactionService).usePoint(eq(user), eq(100), any(), any(), any());
        verify(actionLogService).createActionLog(any(), any(), any(), eq(ActionType.PURCHASE), eq(0));
    }

    @Test
    @DisplayName("에피소드 대여 성공")
    void shouldSaveRentalRecord_whenUserRentsEpisode() {
        // given
        User user = mockUser(1000);
        EpisodePurchaseService.EpisodeInfo episodeInfo = mockEpisodeInfo();
        givenCommonSetup(user, episodeInfo);

        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(any(), any(), any()))
                .thenReturn(Optional.empty());

        EpisodePurchase savedPurchase = mock(EpisodePurchase.class);
        when(savedPurchase.getId()).thenReturn(1L);
        when(episodePurchaseRepository.save(any())).thenReturn(savedPurchase);

        ArgumentCaptor<EpisodePurchase> captor = ArgumentCaptor.forClass(EpisodePurchase.class);

        // when
        episodePurchaseService.createPurchaseHistory(1L, "webnovels", 100L, PurchaseType.RENT);

        // then
        verify(episodePurchaseRepository).save(captor.capture());
        assertEquals(PurchaseType.RENT, captor.getValue().getPurchaseType());
        assertNotNull(captor.getValue().getExpiredAt());
        verify(pointTransactionService).usePoint(eq(user), eq(100), any(), any(), any());
        verify(actionLogService).createActionLog(any(), any(), any(), eq(ActionType.RENTAL), eq(0));
    }

    @Test
    @DisplayName("이미 소장한 에피소드 재구매 시 CustomException 발생")
    void shouldThrowException_whenBuyingAlreadyPurchasedEpisode() {
        // given
        User user = mockUser(1000);
        EpisodePurchaseService.EpisodeInfo episodeInfo = mockEpisodeInfo();

        EpisodePurchase existing = mock(EpisodePurchase.class);
        when(existing.getPurchaseType()).thenReturn(PurchaseType.OWN);

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(episodeProvider.getEpisodeInfo(any(), any())).thenReturn(episodeInfo);
        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> episodePurchaseService.createPurchaseHistory(
                        1L, "webnovels", 100L, PurchaseType.OWN));

        assertEquals(ErrorCode.EPISODE_ALREADY_PURCHASE, ErrorCode.valueOf(exception.getErrorCode()));
    }



    @Test
    @DisplayName("포인트 부족하면 CustomException 발생")
    void shouldThrowException_whenUserHasInsufficientPoints() {
        // given
        User user = mockUser(10); // 포인트 부족
        EpisodePurchaseService.EpisodeInfo episodeInfo = mockEpisodeInfo();

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(episodeProvider.getEpisodeInfo(any(), any())).thenReturn(episodeInfo);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> episodePurchaseService.createPurchaseHistory(
                        1L, "webnovels", 100L, PurchaseType.OWN));

        assertEquals(ErrorCode.INSUFFICIENT_POINTS, ErrorCode.valueOf(exception.getErrorCode()));
        verify(episodePurchaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("대여 중인 에피소드 재대여 시 CustomException 발생")
    void shouldThrowException_whenRentingActiveRentalEpisode() {
        // given
        User user = mockUser(1000);
        EpisodePurchaseService.EpisodeInfo episodeInfo = mockEpisodeInfo();
        givenCommonSetup(user, episodeInfo);

        EpisodePurchase existing = mock(EpisodePurchase.class);
        when(existing.getPurchaseType()).thenReturn(PurchaseType.RENT);
        when(existing.getExpiredAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> episodePurchaseService.createPurchaseHistory(
                        1L, "webnovels", 100L, PurchaseType.RENT));

        assertEquals(ErrorCode.EPISODE_ALREADY_RENTAL, ErrorCode.valueOf(exception.getErrorCode()));
    }

    @Test
    @DisplayName("대여 만료 후 재대여 성공")
    void shouldAllowRerent_whenRentalExpired() {
        // given
        User user = mockUser(1000);
        EpisodePurchaseService.EpisodeInfo episodeInfo = mockEpisodeInfo();
        givenCommonSetup(user, episodeInfo);

        EpisodePurchase existing = mock(EpisodePurchase.class);
        when(existing.getPurchaseType()).thenReturn(PurchaseType.RENT);
        when(existing.getExpiredAt()).thenReturn(LocalDateTime.now().minusDays(1)); // 만료
        when(existing.getId()).thenReturn(1L);
        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        // when
        episodePurchaseService.createPurchaseHistory(1L, "webnovels", 100L, PurchaseType.RENT);

        // then
        verify(existing).extendRental(any(LocalDateTime.class));
        verify(pointTransactionService).usePoint(eq(user), eq(100), any(), any(), any());
        verify(actionLogService).createActionLog(any(), any(), any(), eq(ActionType.RENTAL), eq(0));
    }

    @Test
    @DisplayName("대여 만료 후 소장 업그레이드 성공")
    void shouldUpgradeToPurchase_whenRentalExpiredAndBuying() {
        // given
        User user = mockUser(1000);
        EpisodePurchaseService.EpisodeInfo episodeInfo = mockEpisodeInfo();
        givenCommonSetup(user, episodeInfo);

        EpisodePurchase existing = mock(EpisodePurchase.class);
        when(existing.getPurchaseType()).thenReturn(PurchaseType.RENT);
        when(existing.getExpiredAt()).thenReturn(LocalDateTime.now().minusDays(1)); // 만료
        when(existing.getId()).thenReturn(1L);
        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        // when
        episodePurchaseService.createPurchaseHistory(1L, "webnovels", 100L, PurchaseType.OWN);

        // then
        verify(existing).upgradeToPurchase();
        verify(pointTransactionService).usePoint(eq(user), eq(100), any(), any(), any());
    }

    @Test
    @DisplayName("중복 요청 시 CustomException 발생")
    void shouldThrowException_whenDuplicateRequest() {
        // given
        doThrow(new CustomException(ErrorCode.DUPLICATION_REQUEST))
                .when(idempotentService).isValidIdempotent(any());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> episodePurchaseService.createPurchaseHistory(
                        1L, "webnovels", 100L, PurchaseType.OWN));

        assertEquals(ErrorCode.DUPLICATION_REQUEST, ErrorCode.valueOf(exception.getErrorCode()));
        verify(userRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("소장한 에피소드는 true 반환")
    void checkPurchaseHistory_withOwnedEpisode_shouldReturnTrue() {
        // given
        EpisodePurchase purchase = mock(EpisodePurchase.class);
        when(purchase.getPurchaseType()).thenReturn(PurchaseType.OWN);
        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(1L, 1L, 1L))
                .thenReturn(Optional.of(purchase));

        // when
        Boolean result = episodePurchaseService.checkPurchaseHistory(1L, 1L, 1L);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("유효한 대여 에피소드는 true 반환")
    void checkPurchaseHistory_withActiveRental_shouldReturnTrue() {
        // given
        EpisodePurchase rental = mock(EpisodePurchase.class);
        when(rental.getPurchaseType()).thenReturn(PurchaseType.RENT);
        when(rental.getExpiredAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(1L, 1L, 1L))
                .thenReturn(Optional.of(rental));

        // when
        Boolean result = episodePurchaseService.checkPurchaseHistory(1L, 1L, 1L);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("만료된 대여 에피소드는 false 반환")
    void checkPurchaseHistory_withExpiredRental_shouldReturnFalse() {
        // given
        EpisodePurchase rental = mock(EpisodePurchase.class);
        when(rental.getPurchaseType()).thenReturn(PurchaseType.RENT);
        when(rental.getExpiredAt()).thenReturn(LocalDateTime.now().minusDays(1));
        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(1L, 1L, 1L))
                .thenReturn(Optional.of(rental));

        // when
        Boolean result = episodePurchaseService.checkPurchaseHistory(1L, 1L, 1L);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("구매 기록 없으면 false 반환")
    void checkPurchaseHistory_withNoPurchase_shouldReturnFalse() {
        // given
        when(episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(1L, 1L, 1L))
                .thenReturn(Optional.empty());

        // when
        Boolean result = episodePurchaseService.checkPurchaseHistory(1L, 1L, 1L);

        // then
        assertFalse(result);
    }

}
