package com.pageon.backend.service;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.common.enums.WorkStatus;
import com.pageon.backend.dto.request.ContentRequest;
import com.pageon.backend.dto.response.CreatorContentResponse;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.ContentRepository;
import com.pageon.backend.repository.CreatorRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        lenient().when(fileUploadService.upload(any(), any())).thenReturn("https://s3.url/cover.jpg");
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
        return content;
    }

    @ParameterizedTest
    @DisplayName("콘텐츠 등록 성공")
    @MethodSource("contentTypeSource")
    void createContent_whenValidRequest_shouldSaveContent(String requestContentType, Class<? extends Content> expectedClass) {
        // given
        Creator creator = mock(Creator.class);

        ContentRequest.Create request = new ContentRequest.Create(
                "제목", "설명", requestContentType, "키워드1, 키워드2", LocalDate.now().plusDays(1), mock(MultipartFile.class), WorkStatus.PENDING
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        //when
        creatorContentService.createContent(1L, request);

        // then
        verify(contentRepository).save(captor.capture());
        Content content = captor.getValue();

        assertEquals("제목", content.getTitle());
        assertInstanceOf(expectedClass, content);
        verify(keywordService).registerContentKeyword(eq(content), eq(request.getKeywords()));
        verify(fileUploadService).upload(eq(request.getCoverImage()), anyString());

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
        ContentRequest.Create request = new ContentRequest.Create(
                "제목", "설명", "webnovels", "키워드1, 키워드2", LocalDate.now().plusDays(1), mock(MultipartFile.class), WorkStatus.PENDING
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.createContent(1L, request)
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

        ContentRequest.Create request = new ContentRequest.Create(
                "제목", "설명", "webnovels", "키워드1, 키워드2", LocalDate.now().minusDays(1), mock(MultipartFile.class), WorkStatus.PENDING
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.createContent(1L, request)
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

        ContentRequest.Create request = new ContentRequest.Create(
                "제목", "설명", "all", "키워드1, 키워드2", LocalDate.now().plusDays(1), mock(MultipartFile.class), WorkStatus.PENDING
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.createContent(1L, request)
        );
        
        
        // then
        assertEquals(ErrorCode.INVALID_CONTENT_TYPE, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("지원하지 않는 콘텐츠 타입입니다. webnovel 또는 webtoon만 가능합니다.", exception.getErrorMessage());
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
        when(contentRepository.findByCreator_IdAndStatus(eq(1L), eq(SeriesStatus.ONGOING), any(Pageable.class))).thenReturn(contents);

        // when
        Page<CreatorContentResponse.ContentList> result = creatorContentService.getMyContents(1L, PageRequest.of(0, 10), "ONGOING", "update");


        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(contentRepository).findByCreator_IdAndStatus(eq(1L), eq(SeriesStatus.ONGOING), any(Pageable.class));

    }

    @Test
    @DisplayName("콘텐츠가 없으면 빈 페이지 반환")
    void getMyContents_withNoContents_shouldReturnEmptyPage() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByCreator_IdAndStatus(eq(1L), eq(SeriesStatus.ONGOING), any(Pageable.class))).thenReturn(Page.empty());

        // when
        Page<CreatorContentResponse.ContentList> result = creatorContentService.getMyContents(1L, PageRequest.of(0, 10), "ONGOING", "update");

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
        verify(contentRepository, never()).findByCreator_IdAndStatus(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("검색어가 없으면 크리에이터의 모든 작품 리턴")
    void getSimpleContents_withNoQuery_shouldReturnAllContents() {
        // given
        Content content = mock(Content.class);
        Creator creator = Creator.builder().id(1L).build();
        Page<Content> contents = new PageImpl<>(List.of(content));
        when(creatorRepository.findByUser_Id(eq(1L))).thenReturn(Optional.of(creator));
        when(contentRepository.findByCreator_Id(eq(1L), any(Pageable.class))).thenReturn(contents);

        // when
        Page<CreatorContentResponse.Simple> result = creatorContentService.getSimpleContents(1L, PageRequest.of(0, 10), "");

        // then
        verify(contentRepository, never()).searchByTitle(eq(1L), anyString(), any(Pageable.class));
        assertNotNull(result);

    }

    @Test
    @DisplayName("검색어가 있으면 크리에이터의 검색 작품 리턴")
    void getSimpleContents_withQuery_shouldReturnSearchContents() {
        // given
        Content content = mock(Content.class);
        Creator creator = Creator.builder().id(1L).build();
        Page<Content> contents = new PageImpl<>(List.of(content));
        when(creatorRepository.findByUser_Id(eq(1L))).thenReturn(Optional.of(creator));
        when(contentRepository.searchByTitle(eq(1L), eq("query"), any(Pageable.class))).thenReturn(contents);

        // when
        Page<CreatorContentResponse.Simple> result = creatorContentService.getSimpleContents(1L, PageRequest.of(0, 10), "query");

        // then
        verify(contentRepository, never()).findByCreator_Id(eq(1L), any(Pageable.class));
        assertNotNull(result);
    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void getSimpleContents_whenCreatorNotFound_shouldThrowCustomException() {
        // given

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.getSimpleContents(1L, PageRequest.of(0, 10), "query")
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

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
        when(contentRepository.findByIdAndCreator_Id(eq(1L), eq(1L))).thenReturn(Optional.of(content));
        
        //when
        CreatorContentResponse.Detail result = creatorContentService.getContent(1L, 1L);
        
        // then
        assertNotNull(result);
        assertEquals(1L, result.getContentId());
        assertEquals("제목", result.getContentTitle());
        assertEquals("SF,AI", result.getKeywords());
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
        when(contentRepository.findByIdAndCreator_Id(eq(1L), eq(1L))).thenReturn(Optional.empty());

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

        ContentRequest.Update request = new ContentRequest.Update(
                "수정된 제목", "수정된 설명", "키워드2,키워드3", SerialDay.SATURDAY, null
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_Id(eq(1L), eq(1L))).thenReturn(Optional.of(content));
        doNothing().when(keywordService).updateContentKeyword(any(), any());

        // when
        creatorContentService.updateContent(1L, 1L, request);
        
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
        ContentRequest.Update request = new ContentRequest.Update(
                "수정된 제목", "수정된 설명", "키워드2,키워드3", SerialDay.SATURDAY, multipartFile
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_Id(eq(1L), eq(1L))).thenReturn(Optional.of(content));
        doNothing().when(keywordService).updateContentKeyword(any(), any());

        // when
        creatorContentService.updateContent(1L, 1L, request);

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
        ContentRequest.Update request = new ContentRequest.Update(
                "수정된 제목", "수정된 설명", "키워드2,키워드3", SerialDay.SATURDAY, null
        );
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.updateContent(1L, 1L, request)
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
        when(contentRepository.findByIdAndCreator_Id(eq(1L), eq(1L))).thenReturn(Optional.empty());

        ContentRequest.Update request = new ContentRequest.Update(
                "수정된 제목", "수정된 설명", "키워드2,키워드3", SerialDay.SATURDAY, null
        );
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorContentService.updateContent(1L, 1L, request)
        );

        // then
        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 콘텐츠입니다.", exception.getErrorMessage());

    }

}