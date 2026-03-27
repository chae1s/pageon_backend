package com.pageon.backend.service;

import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.common.enums.WorkStatus;
import com.pageon.backend.dto.request.ContentRequest;
import com.pageon.backend.dto.response.CreatorContentResponse;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.Webnovel;
import com.pageon.backend.entity.Webtoon;
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
        Content content = mock(Content.class);
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

}