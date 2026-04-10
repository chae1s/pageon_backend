package com.pageon.backend.service;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.dto.response.content.ContentDetailResponse;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import com.pageon.backend.repository.content.ContentRepository;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.handler.EpisodeActionHandler;
import com.pageon.backend.service.provider.ContentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DisplayName("ContentService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ContentServiceTest {
    @InjectMocks
    private ContentService contentService;
    @Mock
    private List<ContentProvider> providers;
    @Mock
    private ContentProvider contentProvider;
    @Mock
    private InterestRepository interestRepository;
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ReadingHistoryRepository readingHistoryRepository;
    @Mock
    private EpisodeActionHandler actionHandler;

    @BeforeEach
    void setUp() {
        lenient().when(providers.stream()).thenReturn(Stream.of(contentProvider));
        lenient().when(contentProvider.supports(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("콘텐츠 상세 조회 성공")
    void getContentDetail_withValidContentId_shouldReturnDetail() {
        // given
        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");

        doReturn(Optional.of(content)).when(contentProvider).findById(1L);
        when(contentProvider.findEpisodes(any(), eq(1L))).thenReturn(List.of());
        when(interestRepository.existsByUser_IdAndContentId(any(), eq(1L))).thenReturn(true);

        PrincipalUser principalUser = mock(PrincipalUser.class);
        when(principalUser.getId()).thenReturn(1L);

        // when
        ContentResponse.Detail result = contentService.getContentDetail(principalUser, "novel", 1L);

        // then
        assertNotNull(result);
        verify(contentProvider).findById(1L);
        verify(contentProvider).findEpisodes(1L, 1L);
    }

    @Test
    @DisplayName("콘텐츠가 없으면 CustomException 발생")
    void getContentDetail_withInvalidContentId_shouldThrowCustomException() {
        // given
        doReturn(Optional.empty()).when(contentProvider).findById(1L);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> contentService.getContentDetail(null, "webnovel", 1L)
        );

        // then
        assertEquals("존재하지 않는 콘텐츠입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("비로그인 사용자의 관심 여부는 항상 false 반환")
    void getContentDetail_withNullPrincipalUser_shouldSetIsInterestedFalse() {
        // given
        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");

        doReturn(Optional.of(content)).when(contentProvider).findById(1L);
        when(contentProvider.findEpisodes(any(), eq(1L))).thenReturn(List.of());

        // when
        ContentResponse.Detail result = contentService.getContentDetail(null, "webnovels", 1L);

        // then
        assertNotNull(result);
        assertFalse(result.getIsInterested());
        verify(interestRepository, never()).existsByUser_IdAndContentId(any(), any());

    }

    @Test
    @DisplayName("콘텐츠 상세 조회 성공 - 로그인 유저")
    void getContentDetail_withLoggedInUser_shouldReturnDetail() {
        // given
        ContentDetailResponse contentDetail = mock(ContentDetailResponse.class);

        when(contentRepository.findContentDetail(1L)).thenReturn(Optional.of(contentDetail));
        when(interestRepository.existsByUser_IdAndContentId(1L, 1L)).thenReturn(true);

        // when
        ContentDetailResponse result = contentService.getContentDetail(1L, 1L);

        // then
        assertNotNull(result);
        verify(contentRepository).findContentDetail(1L);
        verify(interestRepository).existsByUser_IdAndContentId(1L, 1L);
        verify(contentDetail).setIsInterested(true);
    }

    @Test
    @DisplayName("콘텐츠 상세 조회 성공 - 비로그인 유저")
    void getContentDetail_withNullUserId_shouldSetIsInterestedFalse() {
        // given
        ContentDetailResponse contentDetail = mock(ContentDetailResponse.class);

        when(contentRepository.findContentDetail(1L)).thenReturn(Optional.of(contentDetail));

        // when
        ContentDetailResponse result = contentService.getContentDetail(null, 1L);

        // then
        assertNotNull(result);
        verify(interestRepository, never())
                .existsByUser_IdAndContentId(any(), any());
        verify(contentDetail).setIsInterested(false);
    }

    @Test
    @DisplayName("관심 없는 콘텐츠면 isInterested false")
    void getContentDetail_withNotInterestedUser_shouldSetIsInterestedFalse() {
        // given
        ContentDetailResponse contentDetail = mock(ContentDetailResponse.class);

        when(contentRepository.findContentDetail(1L)).thenReturn(Optional.of(contentDetail));
        when(interestRepository.existsByUser_IdAndContentId(1L, 1L)).thenReturn(false);

        // when
        contentService.getContentDetail(1L, 1L);

        // then
        verify(contentDetail).setIsInterested(false);
    }

    @Test
    @DisplayName("콘텐츠가 없으면 CustomException 발생")
    void getContentDetail_whenContentNotFound_shouldThrowCustomException() {
        // given
        when(contentRepository.findContentDetail(1L)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> contentService.getContentDetail(1L, 1L));

        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        verify(interestRepository, never()).existsByUser_IdAndContentId(any(), any());
    }

    @Test
    @DisplayName("키워드로 콘텐츠 검색 성공")
    void searchContentsByKeyword_withValidKeyword_shouldReturnContents() {
        // given
        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");
        Page<Content> contents = new PageImpl<>(List.of(content));
        doReturn(contents).when(contentProvider).findByKeyword(eq("SF"), any(Pageable.class));

        // when
        Page<ContentResponse.Search> result = contentService.searchContentsByKeyword("webnovels", "SF", PageRequest.of(0, 10), "latest");

        // then
        assertNotNull(result);
        verify(contentProvider).findByKeyword(eq("SF"), any(Pageable.class));
    }

    @Test
    @DisplayName("빈 키워드로 검색 시 CustomException 발생")
    void searchContentsByKeyword_withBlankKeyword_shouldThrowException() {
        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> contentService.searchContentsByKeyword("webnovels", "  ", PageRequest.of(0, 10), "latest")
        );

        // then
        assertEquals("유효한 키워드가 존재하지 않습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.INVALID_KEYWORD, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("검색어로 콘텐츠 검색 성공")
    void searchContentsByTitleOrAuthor_withValidQuery_shouldReturnContents() {
        // given
        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");

        Page<Content> contents = new PageImpl<>(List.of(content));
        doReturn(contents).when(contentProvider).findByTitleOrPenName(eq("웹소설"), any(Pageable.class));

        // when
        Page<ContentResponse.Search> result = contentService.searchContentsByTitleOrAuthor("webnovels", "웹소설", PageRequest.of(0, 10), "latest");

        // then
        assertNotNull(result);
        verify(contentProvider).findByTitleOrPenName(eq("웹소설"), any(Pageable.class));
    }

    @Test
    @DisplayName("빈 검색어로 콘텐츠 검색 시 CustomException 발생")
    void searchContentsByTitleOrAuthor_withInvalidQuery_shouldThrowCustomException() {

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> contentService.searchContentsByTitleOrAuthor("webnovels", "   ", PageRequest.of(0, 10), "latest")
        );

        // then
        assertEquals("유효한 검색어가 존재하지 않습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.INVALID_SEARCH_QUERY, ErrorCode.valueOf(exception.getErrorCode()));


    }

    @Test
    @DisplayName("신규 콘텐츠 목록 조회 성공")
    void getNewArrivalList_withValidInput_shouldReturnContents() {

        // given
        String contentType = "webnovels";
        LocalDate date = LocalDate.now();

        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");

        Page<Content> contents = new PageImpl<>(List.of(content));

        doReturn(contents).when(contentProvider).findNewArrivals(any(LocalDateTime.class), any(Pageable.class));

        // when
        List<ContentResponse.Simple> result = contentService.getNewArrivalList(contentType, date);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(contentProvider).findNewArrivals(any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    @DisplayName("콘텐츠가 없으면 빈 리스트를 반환한다.")
    void getNewArrivalList_withNoContents_shouldReturnEmptyList() {
        // given
        doReturn(Page.empty()).when(contentProvider).findNewArrivals(any(LocalDateTime.class), any(Pageable.class));

        //when
        List<ContentResponse.Simple> result = contentService.getNewArrivalList("webnovels", LocalDate.now());

        // then
        assertTrue(result.isEmpty());

    }

    @Test
    @DisplayName("지원하지 않는 contentType이면 CustomException 발생")
    void getProvider_withInvalidContentType_shouldThrowCustomException() {
        // given
        when(contentProvider.supports(anyString())).thenReturn(false);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> contentService.getNewArrivalList("webnovels", LocalDate.now())
        );

        // then
        assertEquals("지원하지 않는 콘텐츠 타입입니다. webnovel 또는 webtoon만 가능합니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.INVALID_CONTENT_TYPE, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("요일별 연재 콘텐츠 목룍 조회 성공")
    void getDailyScheduleList_withSerialDay_shouldReturnContents() {
        // given
        String contentType = "webnovels";

        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");

        Page<Content> contents = new PageImpl<>(List.of(content));

        doReturn(contents).when(contentProvider).findBySerialDay(any(), any(Pageable.class));

        // when
        List<ContentResponse.Simple> result = contentService.getDailyScheduleList(contentType, "MONDAY");

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(contentProvider).findBySerialDay(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("콘텐츠가 없으면 빈 리스트를 반환한다.")
    void getDailyScheduleList_withNoContents_shouldReturnEmptyList() {
        // given
        doReturn(Page.empty()).when(contentProvider).findBySerialDay(any(), any(Pageable.class));

        //when
        List<ContentResponse.Simple> result = contentService.getDailyScheduleList("webnovels", "MONDAY");

        // then
        assertTrue(result.isEmpty());

    }

    @Test
    @DisplayName("지원하지 않는 serialDay이면 CustomException 발생")
    void getDailyScheduleList_withInvalidSerialDay_shouldThrowCustomException() {

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> contentService.getDailyScheduleList("webnovels", "DAY")
        );

        // then
        assertEquals("해당하는 요일이 없습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.INVALID_SERIAL_DAY, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("완결 추천작 목록 조회 성공")
    void getBestCompletedList_shouldReturnContents() {
        // given
        String contentType = "webnovels";

        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");

        Page<Content> contents = new PageImpl<>(List.of(content));

        doReturn(contents).when(contentProvider).findByStatusCompleted(any(Pageable.class));

        // when
        List<ContentResponse.Simple> result = contentService.getBestCompletedList(contentType);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(contentProvider).findByStatusCompleted(any(Pageable.class));
    }

    @Test
    @DisplayName("콘텐츠가 없으면 빈 리스트를 반환한다.")
    void getBestCompletedList_withNoContents_shouldReturnEmptyList() {
        // given
        doReturn(Page.empty()).when(contentProvider).findBySerialDay(any(), any(Pageable.class));

        //when
        List<ContentResponse.Simple> result = contentService.getDailyScheduleList("webnovels", "MONDAY");

        // then
        assertTrue(result.isEmpty());

    }

    @Test
    @DisplayName("추천 기간 안에 있는 키워드로 목록 조회 성공")
    void getFeaturedKeywordContentsList_withValidKeyword_shouldReturnContents() {
        // given
        String contentType = "webnovels";
        LocalDate date = LocalDate.now();
        Keyword keyword = Keyword.builder()
                .name("SF")
                .build();
        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");

        Page<Content> contents = new PageImpl<>(List.of(content));

        when(keywordRepository.findValidKeyword(date)).thenReturn(Optional.of(keyword));

        doReturn(contents).when(contentProvider).findByKeyword(any(), any(Pageable.class));

        // when
        ContentResponse.KeywordContent result = contentService.getFeaturedKeywordContentsList(contentType);

        assertNotNull(result);
        assertEquals(keyword.getName(), result.getKeyword());

    }

    @Test
    @DisplayName("추천 기간 안에 키워드가 없으면 CustomException 발생")
    void getFeaturedKeywordContentsList_withNoKeyword_shouldThrowCustomException() {
        // given
        LocalDate date = LocalDate.now();

        when(keywordRepository.findValidKeyword(date)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> contentService.getFeaturedKeywordContentsList("webnovels")
        );

        // then
        assertEquals("유효한 키워드가 존재하지 않습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.INVALID_KEYWORD, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("관심 추가 성공")
    void toggleInterest_whenNotInterested_shouldAddInterest() {
        // given
        User user = mock(User.class);
        Content content = mock(Content.class);

        when(content.getDtype()).thenReturn("WEBNOVEL");

        when(interestRepository.findByUser_IdAndContentId(1L, 1L)).thenReturn(Optional.empty());
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(contentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(content));

        // when
        contentService.toggleInterest(1L, 1L);

        // then
        verify(interestRepository).save(any(Interest.class));
        verify(actionHandler).handleInterestContent(
                eq(1L), eq(1L), eq(ContentType.WEBNOVEL));
    }

    @Test
    @DisplayName("관심 삭제 성공")
    void toggleInterest_whenExistInterest_shouldDeleteInterest() {
        // given
        Interest interest = mock(Interest.class);
        Content content = mock(Content.class);

        when(contentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(content));
        when(interestRepository.findByUser_IdAndContentId(1L, 1L)).thenReturn(Optional.of(interest));

        //when
        contentService.toggleInterest(1L, 1L);

        // then
        verify(interestRepository).delete(interest);

    }

    @Test
    @DisplayName("관심 콘텐츠 목록 조회 성공")
    void getInterestContents_withValidUserId_shouldReturnInterestContents() {
        // given
        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);

        Interest interest = mock(Interest.class);
        when(interest.getContent()).thenReturn(content);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");
        Page<Interest> interests = new PageImpl<>(List.of(interest));

        doReturn(interests).when(contentProvider).findByInterest(eq(1L), any(Pageable.class));

        // when
        Page<ContentResponse.InterestContent> result = contentService.getInterestContents(1L, "webnovels", PageRequest.of(0, 10), "update");

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(contentProvider).findByInterest(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("읽기 기록 조회 성공")
    void getReadingHistory_withValidUserId_shouldReturnReadingHistory() {
        // given
        ReadingHistory history = mock(ReadingHistory.class);
        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);

        when(history.getContent()).thenReturn(content);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");

        Page<ReadingHistory> historyPage = new PageImpl<>(List.of(history));
        doReturn(historyPage).when(contentProvider)
                .findByReadingHistory(eq(1L), any(Pageable.class));

        // when
        Page<ContentResponse.RecentRead> result = contentService.getReadingHistory(
                1L, "webnovels", "recently_read", PageRequest.of(0, 10));

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(contentProvider).findByReadingHistory(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("오늘 읽은 기록 조회 성공")
    void getTodayReadingHistory_withValidUserId_shouldReturnTodayHistory() {
        // given
        Content content = mock(Content.class);
        Creator creator = mock(Creator.class);
        ReadingHistory history = mock(ReadingHistory.class);
        when(history.getContent()).thenReturn(content);
        when(content.getCreator()).thenReturn(creator);
        when(creator.getPenName()).thenReturn("penName");

        SerialDay today = SerialDay.valueOf(LocalDate.now().getDayOfWeek().name());

        when(readingHistoryRepository.findWithContentByUserIdAndSerialDay(eq(1L), eq(today)))
                .thenReturn(List.of(history));

        // when
        List<ContentResponse.Simple> result = contentService.getTodayReadingHistory(1L);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(readingHistoryRepository)
                .findWithContentByUserIdAndSerialDay(eq(1L), eq(today));
    }
}