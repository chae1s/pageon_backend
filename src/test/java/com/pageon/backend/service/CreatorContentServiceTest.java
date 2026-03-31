package com.pageon.backend.service;

import com.pageon.backend.common.enums.*;
import com.pageon.backend.dto.request.content.ContentCreate;
import com.pageon.backend.dto.request.content.ContentDelete;
import com.pageon.backend.dto.request.content.ContentUpdate;
import com.pageon.backend.dto.response.creator.content.*;
import com.pageon.backend.dto.response.creator.deletion.DeletionList;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.ContentDeletionRequestRepository;
import com.pageon.backend.repository.ContentRepository;
import com.pageon.backend.repository.CreatorRepository;
import com.pageon.backend.service.provider.EpisodeProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ActiveProfiles("test")
@DisplayName("CreatorContentService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CreatorContentServiceTest {
    @InjectMocks
    private CreatorContentService creatorContentService;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private FileUploadService fileUploadService;
    @Mock
    private KeywordService keywordService;
    @Mock
    private ContentDeletionRequestRepository contentDeletionRequestRepository;
    @Mock
    private List<EpisodeProvider> providers;
    @Mock
    private EpisodeProvider episodeProvider;

    @BeforeEach
    void setUp() {
        lenient().when(fileUploadService.upload(any(), any())).thenReturn("https://s3.url/cover.jpg");
        lenient().when(providers.stream()).thenReturn(Stream.of(episodeProvider));
        lenient().when(episodeProvider.supports(anyString())).thenReturn(true);
    }

    private Content mockContent() {
        Content content = mock(Content.class);
        lenient().when(content.getId()).thenReturn(1L);
        lenient().when(content.getTitle()).thenReturn("제목");
        lenient().when(content.getDescription()).thenReturn("설명");
        lenient().when(content.getCover()).thenReturn("cover.jpg");
        lenient().when(content.getSerialDay()).thenReturn(SerialDay.MONDAY);
        lenient().when(content.getContentKeywords()).thenReturn(List.of());
        lenient().when(content.getDtype()).thenReturn("WEBNOVEL");
        lenient().when(content.getStatus()).thenReturn(SeriesStatus.ONGOING);
        lenient().when(content.getContentKeywords()).thenReturn(List.of());
        lenient().when(content.getViewCount()).thenReturn(100L);
        lenient().when(content.getInterestCount()).thenReturn(100L);
        lenient().when(content.getWorkStatus()).thenReturn(WorkStatus.PENDING);
        lenient().when(content.getEpisodeUpdatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(content.getEpisodeCount()).thenReturn(20);
        return content;
    }

    private ContentDeletionRequest mockDeletionRequest() {
        Content content = mockContent();

        ContentDeletionRequest deletionRequest = mock(ContentDeletionRequest.class);
        when(deletionRequest.getId()).thenReturn(1L);
        when(deletionRequest.getContent()).thenReturn(content);
        when(deletionRequest.getDeleteReason()).thenReturn(DeleteReason.STORY_ISSUE);
        when(deletionRequest.getReasonDetail()).thenReturn("상세 이유");
        when(deletionRequest.getRequestedAt()).thenReturn(LocalDateTime.now());
        when(deletionRequest.getDeleteStatus()).thenReturn(DeleteStatus.PENDING);
        return deletionRequest;
    }

    @ParameterizedTest
    @DisplayName("콘텐츠 등록 성공")
    @MethodSource("contentTypeSource")
    void createContent_whenValidRequest_shouldSaveContent(String requestContentType, Class<? extends Content> expectedClass) {
        // given
        Creator creator = mock(Creator.class);

        ContentCreate request = new ContentCreate(
                "제목", "설명", requestContentType, "키워드1, 키워드2", LocalDate.now().plusDays(1), WorkStatus.PENDING
        );
        MultipartFile file = mock(MultipartFile.class);
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        //when
        creatorContentService.createContent(1L, request, file);

        // then
        verify(contentRepository).save(captor.capture());
        Content content = captor.getValue();

        assertEquals("제목", content.getTitle());
        assertInstanceOf(expectedClass, content);
        verify(keywordService).registerContentKeyword(eq(content), eq(request.getKeywords()));
        verify(fileUploadService).upload(eq(file), anyString());

    }

    static Stream<Arguments> contentTypeSource() {
        return Stream.of(
                Arguments.of("webnovels", Webnovel.class),
                Arguments.of("webtoons", Webtoon.class)
        );
    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void createContent_whenCreatorNotFound_shouldThrowCustomException() {
        // given
        ContentCreate request = new ContentCreate(
                "제목", "설명", "webnovels", "키워드1, 키워드2", LocalDate.now().plusDays(1), WorkStatus.PENDING
        );
        MultipartFile file = mock(MultipartFile.class);
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.createContent(1L, request, file)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());
        verify(contentRepository, never()).save(any());
    }

    @Test
    @DisplayName("연재일이 현재 시간 이전이면 CustomException 발생")
    void creatorContent_whenInvalidPublishedAt_shouldThrowCustomException() {
        // given
        Creator creator = mock(Creator.class);

        ContentCreate request = new ContentCreate(
                "제목", "설명", "webnovels", "키워드1, 키워드2", LocalDate.now().minusDays(1), WorkStatus.PENDING
        );
        MultipartFile file = mock(MultipartFile.class);

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.createContent(1L, request, file)
        );
        
        // then
        assertEquals(ErrorCode.INVALID_PUBLISHED_AT, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("연재 시작일이 유효하지 않습니다.", exception.getErrorMessage());
        verify(contentRepository, never()).save(any());

    }
    
    @Test
    @DisplayName("지원하는 contentType이 아니면 CustomException 발생")
    void createContent_whenInvalidContentType_shouldThrowCustomException() {
        Creator creator = mock(Creator.class);

        ContentCreate request = new ContentCreate(
                "제목", "설명", "all", "키워드1, 키워드2", LocalDate.now().plusDays(1), WorkStatus.PENDING
        );
        MultipartFile file = mock(MultipartFile.class);

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.createContent(1L, request, file)
        );
        
        
        // then
        assertEquals(ErrorCode.INVALID_CONTENT_TYPE, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("지원하지 않는 콘텐츠 타입입니다. webnovel 또는 webtoon만 가능합니다.", exception.getErrorMessage());
        verify(contentRepository, never()).save(any());
        
        
    }

    @Test
    @DisplayName("업로드된 커버 이미지가 없으면 CustomException 발생")
    void createContent_withNotExitingFile_shouldCustomException() {

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.createContent(1L, new ContentCreate(), null)
        );


        // then
        assertEquals(ErrorCode.FILE_EMPTY, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("업로드된 파일이 없습니다.", exception.getErrorMessage());
        verify(contentRepository, never()).save(any());

    }

    @Test
    @DisplayName("크리에이터 콘텐츠 목록 조회 성공")
    void getMyContents_withValidUserId_shouldReturnContents() {
        // given
        Content content = mock(Content.class);
        Creator creator = Creator.builder().id(1L).build();
        Page<Content> contents = new PageImpl<>(List.of(content));
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByCreator_IdAndStatusAndDeletedAtIsNull(eq(1L), eq(SeriesStatus.ONGOING), any(Pageable.class))).thenReturn(contents);

        // when
        Page<ContentList> result = creatorContentService.getMyContents(1L, PageRequest.of(0, 10), "ONGOING", "update");


        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(contentRepository).findByCreator_IdAndStatusAndDeletedAtIsNull(eq(1L), eq(SeriesStatus.ONGOING), any(Pageable.class));

    }

    @Test
    @DisplayName("콘텐츠가 없으면 빈 페이지 반환")
    void getMyContents_withNoContents_shouldReturnEmptyPage() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByCreator_IdAndStatusAndDeletedAtIsNull(eq(1L), eq(SeriesStatus.ONGOING), any(Pageable.class))).thenReturn(Page.empty());

        // when
        Page<ContentList> result = creatorContentService.getMyContents(1L, PageRequest.of(0, 10), "ONGOING", "update");

        // then
        assertTrue(result.isEmpty());

    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void getMyContents_whenCreatorNotFound_shouldThrowCustomException() {
        // given

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.getMyContents(1L, PageRequest.of(0, 10), "ONGOING", "update")
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());
        verify(contentRepository, never()).findByCreator_IdAndStatusAndDeletedAtIsNull(any(), any(), any(Pageable.class));
    }
    
    @Test
    @DisplayName("크리에이터 콘텐츠 상세 조회 성공")
    void getContent_withCreatorAndContentId_shouldReturnContent() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        Keyword keyword1 = Keyword.builder().name("SF").build();
        Keyword keyword2 = Keyword.builder().name("AI").build();
        ContentKeyword ck1 = ContentKeyword.builder().keyword(keyword1).build();
        ContentKeyword ck2 = ContentKeyword.builder().keyword(keyword2).build();

        Content content = mockContent();
        when(content.getContentKeywords()).thenReturn(List.of(ck1, ck2));
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(content));
        
        //when
        ContentDetail result = creatorContentService.getContent(1L, 1L);
        
        // then
        assertNotNull(result);
        assertEquals(1L, result.getContentId());
        assertEquals("제목", result.getContentTitle());
        assertEquals("SF,AI", result.getKeywordLine());
        assertEquals(SerialDay.MONDAY, result.getSerialDay());
        
    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void getContent_whenCreatorNotFound_shouldThrowCustomException() {
        // given

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.getContent(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("콘텐츠가 없으면 CustomException 발생")
    void getContent_whenContentNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.getContent(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 콘텐츠입니다.", exception.getErrorMessage());

    }
    
    @Test
    @DisplayName("콘텐츠 수정 성공 - 커버 이미지는 수정하지 않음")
    void updateContent_withoutCoverImage_shouldUpdateContent() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Content content = mockContent();

        ContentUpdate request = new ContentUpdate(
                "수정된 제목", "수정된 설명", "키워드2,키워드3", SerialDay.SATURDAY, SeriesStatus.ONGOING
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(content));
        doNothing().when(keywordService).updateContentKeyword(any(), any());

        // when
        creatorContentService.updateContent(1L, 1L, request, null);
        
        // then
        verify(keywordService).updateContentKeyword(eq(content), eq("키워드2,키워드3"));
        verify(fileUploadService, never()).deleteFile(any());
        verify(fileUploadService, never()).upload(any(), any());
        verify(content).updateContent(request);
        
    }

    @Test
    @DisplayName("콘텐츠 수정 성공 - 커버 이미지 수정")
    void updateContent_withCoverImage_shouldUpdateContent() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Content content = mockContent();
        MultipartFile multipartFile = mock(MultipartFile.class);
        ContentUpdate request = new ContentUpdate(
                "수정된 제목", "수정된 설명", "키워드2,키워드3", SerialDay.SATURDAY, SeriesStatus.ONGOING
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(content));
        doNothing().when(keywordService).updateContentKeyword(any(), any());

        // when
        creatorContentService.updateContent(1L, 1L, request, multipartFile);

        // then
        verify(keywordService).updateContentKeyword(eq(content), eq("키워드2,키워드3"));
        verify(fileUploadService).deleteFile("cover.jpg");
        verify(fileUploadService).upload(eq(multipartFile), anyString());
        verify(content).updateContent(request);

    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void updateContent_whenCreatorNotFound_shouldThrowCustomException() {
        // given

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        ContentUpdate request = new ContentUpdate(
                "수정된 제목", "수정된 설명", "키워드2,키워드3", SerialDay.SATURDAY, SeriesStatus.ONGOING
        );
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.updateContent(1L, 1L, request, null)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("콘텐츠가 없으면 CustomException 발생")
    void updateContent_whenContentNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.empty());

        ContentUpdate request = new ContentUpdate(
                "수정된 제목", "수정된 설명", "키워드2,키워드3", SerialDay.SATURDAY, SeriesStatus.ONGOING
        );
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.updateContent(1L, 1L, request, null)
        );

        // then
        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 콘텐츠입니다.", exception.getErrorMessage());

    }


    @Test
    @DisplayName("콘텐츠 삭제 요청 성공")
    void requestContentDeletion_withValidInput_shouldSaveDeletionRequest() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Content content = mockContent();

        ContentDelete request = new ContentDelete(DeleteReason.STORY_ISSUE, "상세 이유");

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L)))
                .thenReturn(Optional.of(content));

        ArgumentCaptor<ContentDeletionRequest> captor =
                ArgumentCaptor.forClass(ContentDeletionRequest.class);

        // when
        creatorContentService.requestContentDeletion(1L, 1L, request);

        // then
        verify(content).deletionRequest();
        verify(contentDeletionRequestRepository).save(captor.capture());

        ContentDeletionRequest saved = captor.getValue();
        assertEquals(content, saved.getContent());
        assertEquals(creator, saved.getCreator());
        assertEquals(DeleteReason.STORY_ISSUE, saved.getDeleteReason());
        assertEquals("상세 이유", saved.getReasonDetail());
        assertEquals(DeleteStatus.PENDING, saved.getDeleteStatus());
        assertNotNull(saved.getRequestedAt());
    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void requestContentDeletion_whenCreatorNotFound_shouldThrowCustomException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        ContentDelete request = new ContentDelete(DeleteReason.STORY_ISSUE, "상세 이유");
        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.requestContentDeletion(1L, 1L, request)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("콘텐츠가 없으면 CustomException 발생")
    void requestContentDeletion_whenContentNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        ContentDelete request = new ContentDelete(DeleteReason.STORY_ISSUE, "상세 이유");
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.requestContentDeletion(1L, 1L, request)
        );

        // then
        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 콘텐츠입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("삭제 요청 목록 조회 성공")
    void getMyDeletionRequests_withValidUserId_shouldReturnDeletionList() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        ContentDeletionRequest deletionRequest = mockDeletionRequest();

        Page<ContentDeletionRequest> deletionRequests =
                new PageImpl<>(List.of(deletionRequest));

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentDeletionRequestRepository.findByCreatorId(eq(1L), any(Pageable.class)))
                .thenReturn(deletionRequests);

        // when
        Page<DeletionList> result =
                creatorContentService.getMyDeletionRequests(1L, PageRequest.of(0, 10));

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        DeletionList item = result.getContent().get(0);
        assertEquals(1L, item.getId());
        assertEquals("제목", item.getContentTitle());
        assertEquals(DeleteReason.STORY_ISSUE, item.getDeleteReason());
        assertEquals(DeleteStatus.PENDING, item.getDeleteStatus());

        verify(contentDeletionRequestRepository)
                .findByCreatorId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("삭제 요청이 없으면 빈 페이지 반환")
    void getMyDeletionRequests_withNoDeletionRequests_shouldReturnEmptyPage() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentDeletionRequestRepository.findByCreatorId(eq(1L), any(Pageable.class)))
                .thenReturn(Page.empty());

        // when
        Page<DeletionList> result =
                creatorContentService.getMyDeletionRequests(1L, PageRequest.of(0, 10));

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void getMyDeletionRequests_whenCreatorNotFound_shouldThrowException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.getMyDeletionRequests(1L, PageRequest.of(0, 10)));

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("삭제 요청 취소 성공")
    void cancelDeletionRequest_withValidInput_shouldCancelRequest() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Content content = mockContent();

        ContentDeletionRequest deletionRequest = mock(ContentDeletionRequest.class);
        when(deletionRequest.getDeleteStatus()).thenReturn(DeleteStatus.PENDING);
        when(deletionRequest.getContent()).thenReturn(content);

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentDeletionRequestRepository.findByIdAndCreator_Id(eq(1L), eq(1L)))
                .thenReturn(Optional.of(deletionRequest));

        // when
        creatorContentService.cancelDeletionRequest(1L, 1L);

        // then
        verify(content).cancelDeletion();
        verify(deletionRequest).cancelDeletion();
    }

    @Test
    @DisplayName("PENDING 상태가 아니면 CustomException 발생")
    void cancelDeletionRequest_whenNotPending_shouldThrowException() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        ContentDeletionRequest deletionRequest = mock(ContentDeletionRequest.class);
        when(deletionRequest.getDeleteStatus()).thenReturn(DeleteStatus.APPROVED); // ← PENDING 아님

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentDeletionRequestRepository.findByIdAndCreator_Id(eq(1L), eq(1L)))
                .thenReturn(Optional.of(deletionRequest));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.cancelDeletionRequest(1L, 1L));

        // then
        assertEquals(ErrorCode.INVALID_CANCEL_DELETE_REQUEST, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("콘텐츠 삭제 요청을 취소할 수 없습니다.", exception.getErrorMessage());
        verify(deletionRequest, never()).cancelDeletion();
    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void cancelDeletionRequest_whenCreatorNotFound_shouldThrowException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.cancelDeletionRequest(1L, 1L));

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("삭제 요청이 없으면 CustomException 발생")
    void cancelDeletionRequest_whenDeletionRequestNotFound_shouldThrowException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentDeletionRequestRepository.findByIdAndCreator_Id(eq(1L), eq(1L)))
                .thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.cancelDeletionRequest(1L, 1L));

        // then
        assertEquals(ErrorCode.DELETION_REQUEST_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("콘텐츠 삭제 요청이 존재하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("콘텐츠 통계 조회 성공")
    void getContentStats_withValidUserId_shouldReturnStats() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        List<Object[]> statusRows = List.of(
                new Object[]{SeriesStatus.ONGOING, 3L},
                new Object[]{SeriesStatus.REST, 2L},
                new Object[]{SeriesStatus.COMPLETED, 1L}
        );

        ContentPerformanceStats stats = new ContentPerformanceStats(100L, 9.5, 109L);
        List<ContentTab> contentTabs = List.of(mock(ContentTab.class), mock(ContentTab.class));

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.countGroupByStatus(eq(1L))).thenReturn(statusRows);
        when(contentDeletionRequestRepository.countByCreator_IdAndDeleteStatus(eq(1L), eq(DeleteStatus.PENDING))).thenReturn(1);
        when(contentRepository.findTotalStatsByCreatorId(eq(1L))).thenReturn(stats);
        when(contentRepository.findAllByStatusOngoing(eq(1L))).thenReturn(contentTabs);

        // when
        ContentStats result = creatorContentService.getContentStats(1L);

        // then
        assertNotNull(result);
        verify(contentRepository).countGroupByStatus(eq(1L));
        verify(contentDeletionRequestRepository)
                .countByCreator_IdAndDeleteStatus(eq(1L), eq(DeleteStatus.PENDING));
        verify(contentRepository).findTotalStatsByCreatorId(eq(1L));
        verify(contentRepository).findAllByStatusOngoing(eq(1L));
        assertEquals(3L, result.getOngoingCount());
        assertEquals(2L, result.getRestCount());
        assertEquals(1L, result.getCompletedCount());
    }

    @Test
    @DisplayName("콘텐츠가 없으면 빈 statusMap 반환")
    void getContentStats_withNoContents_shouldReturnEmptyStatusMap() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.countGroupByStatus(1L)).thenReturn(List.of()); // 비어있음
        when(contentDeletionRequestRepository.countByCreator_IdAndDeleteStatus(any(), any()))
                .thenReturn(0);
        when(contentRepository.findTotalStatsByCreatorId(1L))
                .thenReturn(mock(ContentPerformanceStats.class));
        when(contentRepository.findAllByStatusOngoing(1L)).thenReturn(List.of());

        // when
        ContentStats result = creatorContentService.getContentStats(1L);

        // then
        assertNotNull(result);
        assertEquals(0, result.getOngoingCount());
    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void getContentStats_whenCreatorNotFound_shouldThrowException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.getContentStats(1L));

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());
        verify(contentRepository, never()).countGroupByStatus(any());
        verify(contentRepository, never()).findTotalStatsByCreatorId(any());
    }

    @Test
    @DisplayName("콘텐츠 대시보드 조회 성공")
    void getContentDashboard_withValidUserId_shouldReturnContentDashboard() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Content content = mockContent();

        List<Object[]> statusRows = List.of(
                new Object[]{EpisodeStatus.SCHEDULED, 10L},
                new Object[]{EpisodeStatus.PUBLISHED, 20L},
                new Object[]{EpisodeStatus.DRAFT, 30L}
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(content));
        when(episodeProvider.getGroupByStats(eq(1L))).thenReturn(statusRows);

        //when
        ContentDashboard result = creatorContentService.getContentDashboard(1L, 1L);

        // then
        assertNotNull(result);
        assertEquals(20, result.getEpisodeStats().getPublishedEpisodeCount());
        assertEquals(30, result.getEpisodeStats().getDraftEpisodeCount());
        assertEquals("cover.jpg", result.getCover());


    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void getContentDashboard_whenCreatorNotFound_shouldThrowException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.getContentDashboard(1L, 1L));

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());
        verify(episodeProvider, never()).getGroupByStats(any());
    }

    @Test
    @DisplayName("콘텐츠가 없으면 CustomException 발생")
    void getContentDashboard_whenContentNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.getContentDashboard(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 콘텐츠입니다.", exception.getErrorMessage());
        verify(episodeProvider, never()).getGroupByStats(any());
    }


}