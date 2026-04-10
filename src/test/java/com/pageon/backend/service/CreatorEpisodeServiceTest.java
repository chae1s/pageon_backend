package com.pageon.backend.service;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.request.episode.WebnovelEpisodeCreate;
import com.pageon.backend.dto.request.episode.WebnovelEpisodeUpdate;
import com.pageon.backend.dto.request.episode.WebtoonEpisodeCreate;
import com.pageon.backend.dto.request.episode.WebtoonEpisodeUpdate;
import com.pageon.backend.dto.response.creator.episode.EpisodeDashboard;
import com.pageon.backend.dto.response.creator.episode.EpisodeList;
import com.pageon.backend.dto.response.creator.episode.WebnovelEpisodeDetail;
import com.pageon.backend.dto.response.creator.episode.WebtoonEpisodeDetail;
import com.pageon.backend.dto.response.episode.EpisodeImage;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import com.pageon.backend.repository.content.ContentRepository;
import com.pageon.backend.repository.episode.WebnovelEpisodeRepository;
import com.pageon.backend.repository.WebtoonRepository;
import com.pageon.backend.repository.episode.WebtoonEpisodeRepository;
import com.pageon.backend.service.provider.EpisodeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ActiveProfiles("test")
@DisplayName("CreatorEpisodeService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CreatorEpisodeServiceTest {
    @InjectMocks
    private CreatorEpisodeService creatorEpisodeService;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private WebnovelRepository webnovelRepository;
    @Mock
    private WebtoonRepository webtoonRepository;
    @Mock
    private WebnovelEpisodeRepository webnovelEpisodeRepository;
    @Mock
    private WebtoonEpisodeRepository webtoonEpisodeRepository;
    @Mock
    private WebtoonImageService webtoonImageService;
    @Mock
    private List<EpisodeProvider> providers;
    @Mock
    private EpisodeProvider episodeProvider;
    @Mock
    private ContentRepository contentRepository;

    @BeforeEach
    void setUp() {
        lenient().when(providers.stream()).thenReturn(Stream.of(episodeProvider));
        lenient().when(episodeProvider.supports(anyString())).thenReturn(true);
    }

    private WebnovelEpisode mockWebnovelEpisode(Creator creator) {
        WebnovelEpisode episode = mock(WebnovelEpisode.class);
        Webnovel webnovel = mock(Webnovel.class);
        lenient().when(webnovel.getTitle()).thenReturn("웹소설 제목");
        lenient().when(webnovel.getCreator()).thenReturn(creator);

        lenient().when(episode.getWebnovel()).thenReturn(webnovel);
        lenient().when(episode.getEpisodeNum()).thenReturn(10);
        lenient().when(episode.getId()).thenReturn(1L);
        lenient().when(episode.getEpisodeTitle()).thenReturn("에피소드 제목");
        lenient().when(episode.getPublishedAt()).thenReturn(LocalDate.now());
        lenient().when(episode.getEpisodeStatus()).thenReturn(EpisodeStatus.PUBLISHED);
        lenient().when(episode.getContent()).thenReturn("에피소드 내용");
        lenient().when(episode.getCreatedAt()).thenReturn(LocalDateTime.now());

        return episode;
    }

    private WebtoonEpisode mockWebtoonEpisode(Creator creator) {
        WebtoonEpisode episode = mock(WebtoonEpisode.class);
        Webtoon webtoon = mock(Webtoon.class);
        lenient().when(webtoon.getTitle()).thenReturn("웹툰 제목");
        lenient().when(webtoon.getCreator()).thenReturn(creator);

        lenient().when(episode.getWebtoon()).thenReturn(webtoon);
        lenient().when(episode.getEpisodeNum()).thenReturn(10);
        lenient().when(episode.getId()).thenReturn(1L);
        lenient().when(episode.getEpisodeTitle()).thenReturn("에피소드 제목");
        lenient().when(episode.getPublishedAt()).thenReturn(LocalDate.now());
        lenient().when(episode.getEpisodeStatus()).thenReturn(EpisodeStatus.PUBLISHED);
        lenient().when(episode.getCreatedAt()).thenReturn(LocalDateTime.now());

        return episode;
    }


    private Creator mockCreator() {
        return Creator.builder().id(1L).build();
    }

    @Test
    @DisplayName("웹소설 에피소드 생성 성공")
    void createWebnovelEpisode_withValidInput_shouldSaveEpisode() {
        // given
        Creator creator = mockCreator();
        Webnovel webnovel = mock(Webnovel.class);
        WebnovelEpisodeCreate request = new WebnovelEpisodeCreate(
                "에피소드 제목", LocalDate.now().plusDays(1), "에피소드 내용"
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(webnovel));
        when(webnovelEpisodeRepository.findMaxEpisodeNumByContentId(eq(1L))).thenReturn(Optional.of(5));

        ArgumentCaptor<WebnovelEpisode> captor = ArgumentCaptor.forClass(WebnovelEpisode.class);

        //when
        creatorEpisodeService.createWebnovelEpisode(1L, 1L, request);

        // then
        verify(webnovelEpisodeRepository).save(captor.capture());
        WebnovelEpisode episode = captor.getValue();

        assertEquals(6, episode.getEpisodeNum());
        assertEquals("에피소드 제목", episode.getEpisodeTitle());
        assertEquals(EpisodeStatus.SCHEDULED, episode.getEpisodeStatus());
        assertEquals(webnovel, episode.getWebnovel());

    }

    @Test
    @DisplayName("첫 번째 웹소설 에피소드 생성 시 1화로 저장")
    void createWebnovelEpisode_withNoExistingEpisode_shouldSaveAsFirstEpisode() {
        // given
        Creator creator = mockCreator();
        Webnovel webnovel = mock(Webnovel.class);
        WebnovelEpisodeCreate request = new WebnovelEpisodeCreate(
                "에피소드 제목", LocalDate.now().plusDays(1), "에피소드 내용"
        );

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(webnovel));
        when(webnovelEpisodeRepository.findMaxEpisodeNumByContentId(eq(1L))).thenReturn(Optional.empty());

        ArgumentCaptor<WebnovelEpisode> captor = ArgumentCaptor.forClass(WebnovelEpisode.class);

        // when
        creatorEpisodeService.createWebnovelEpisode(1L, 1L, request);

        // then
        verify(webnovelEpisodeRepository).save(captor.capture());
        WebnovelEpisode episode = captor.getValue();

        assertEquals(1, episode.getEpisodeNum());
        assertEquals("에피소드 제목", episode.getEpisodeTitle());
        assertEquals(EpisodeStatus.SCHEDULED, episode.getEpisodeStatus());
        assertEquals(webnovel, episode.getWebnovel());


    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void createWebnovelEpisode_whenCreatorNotFound_shouldThrowCustomException() {
        // given

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.createWebnovelEpisode(1L, 1L, new WebnovelEpisodeCreate())
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());
        verify(webnovelEpisodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("웹소설이 없으면 CustomException 발생")
    void createWebnovelEpisode_withNoContents_shouldThrowCustomException() {
        // given
        Creator creator = mockCreator();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.createWebnovelEpisode(1L, 1L, new WebnovelEpisodeCreate())
        );

        // then
        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 콘텐츠입니다.", exception.getErrorMessage());
        verify(webnovelEpisodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("웹툰 에피소드 생성 성공")
    void createWebtoonEpisode_withValidInput_shouldSaveEpisode() {
        // given
        Creator creator = mockCreator();
        Webtoon webtoon = mock(Webtoon.class);
        WebtoonEpisodeCreate request = new WebtoonEpisodeCreate(
                "에피소드 제목", LocalDate.now().plusDays(1)
        );

        MultipartFile[] files = {mock(MultipartFile.class)};

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(webtoon));
        when(webtoonEpisodeRepository.findMaxEpisodeNumByContentId(eq(1L))).thenReturn(Optional.of(5));

        ArgumentCaptor<WebtoonEpisode> captor = ArgumentCaptor.forClass(WebtoonEpisode.class);


        //when
        creatorEpisodeService.createWebtoonEpisode(1L, 1L, request, files);

        // then
        verify(webtoonEpisodeRepository).save(captor.capture());
        WebtoonEpisode episode = captor.getValue();

        assertEquals(6, episode.getEpisodeNum());
        assertEquals("에피소드 제목", episode.getEpisodeTitle());
        assertEquals(EpisodeStatus.SCHEDULED, episode.getEpisodeStatus());
        assertEquals(webtoon, episode.getWebtoon());
        verify(webtoonImageService).registerWebtoonImage(eq(1L), eq(episode), eq(files));

    }

    @Test
    @DisplayName("첫 번째 웹툰 에피소드 생성 시 1화로 저장")
    void createWebtoonEpisode_withNotExistingEpisode_shouldSaveAsFirstEpisode() {
        // given
        Creator creator = mockCreator();
        Webtoon webtoon = mock(Webtoon.class);
        WebtoonEpisodeCreate request = new WebtoonEpisodeCreate(
                "에피소드 제목", LocalDate.now().plusDays(1)
        );

        MultipartFile[] files = {mock(MultipartFile.class)};

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(webtoon));
        when(webtoonEpisodeRepository.findMaxEpisodeNumByContentId(eq(1L))).thenReturn(Optional.empty());

        ArgumentCaptor<WebtoonEpisode> captor = ArgumentCaptor.forClass(WebtoonEpisode.class);


        //when
        creatorEpisodeService.createWebtoonEpisode(1L, 1L, request, files);

        // then
        verify(webtoonEpisodeRepository).save(captor.capture());
        WebtoonEpisode episode = captor.getValue();

        assertEquals(1, episode.getEpisodeNum());
        assertEquals("에피소드 제목", episode.getEpisodeTitle());
        assertEquals(EpisodeStatus.SCHEDULED, episode.getEpisodeStatus());
        assertEquals(webtoon, episode.getWebtoon());
        verify(webtoonImageService).registerWebtoonImage(eq(1L), eq(episode), eq(files));


    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void createWebtoonEpisode_whenCreatorNotFound_shouldThrowCustomException() {
        // given
        MultipartFile[] files = {mock(MultipartFile.class)};

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.createWebtoonEpisode(1L, 1L, new WebtoonEpisodeCreate(), files)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());
        verify(webtoonEpisodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("웹툰이 없으면 CustomException 발생")
    void createWebtoonEpisode_withNoContents_shouldThrowCustomException() {
        // given
        Creator creator = mockCreator();
        MultipartFile[] files = {mock(MultipartFile.class)};

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.createWebtoonEpisode(1L, 1L, new WebtoonEpisodeCreate(), files)
        );

        // then
        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 콘텐츠입니다.", exception.getErrorMessage());
        verify(webtoonEpisodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("업로드된 웹툰 에피소드 이미지가 없으면 CustomException 발생")
    void createWebtoonEpisode_withNotExitingFiles_shouldCustomException() {

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.createWebtoonEpisode(1L, 1L, new WebtoonEpisodeCreate(), null)
        );


        // then
        assertEquals(ErrorCode.FILE_EMPTY, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("업로드된 파일이 없습니다.", exception.getErrorMessage());
        verify(webtoonEpisodeRepository, never()).save(any());

    }

    @Test
    @DisplayName("에피소드 대시보드 조회 성공 - 에피소드 상태 ALL 조회")
    void getEpisodeDashboard_withAllStatus_shouldReturnDashboard() {
        // given
        Creator creator = mockCreator();
        Content content = mock(Content.class);
        when(content.getId()).thenReturn(1L);

        Page<EpisodeList> episodePage = new PageImpl<>(List.of(mock(EpisodeList.class)));

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(content));
        when(episodeProvider.getAllEpisodesByContent(eq(1L), any(Pageable.class))).thenReturn(episodePage);
        when(content.getDtype()).thenReturn("WEBNOVEL");
        when(content.getTitle()).thenReturn("제목");
        when(content.getEpisodeCount()).thenReturn(20);
        when(episodeProvider.getGroupByStats(eq(1L))).thenReturn(List.of());

        //when
        EpisodeDashboard result = creatorEpisodeService.getEpisodeDashboard(1L, 1L, PageRequest.of(0, 10), "ALL", "latest");

        // then
        assertNotNull(result);
        assertEquals("제목", result.getContentTitle());
        assertEquals("webnovels", result.getContentType());
        verify(episodeProvider, never()).getEpisodesByEpisodeStatus(eq(1L), any(), any(Pageable.class));

    }

    @Test
    @DisplayName("에피소드 대시보드 조회 성공 - 특정 에피소드 상태 조회")
    void getEpisodeDashboard_withSpecificStatus_shouldReturnDashboard() {
        // given
        Creator creator = mockCreator();
        Content content = mock(Content.class);
        when(content.getId()).thenReturn(1L);

        Page<EpisodeList> episodePage = new PageImpl<>(List.of(mock(EpisodeList.class)));

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.of(content));
        when(episodeProvider.getEpisodesByEpisodeStatus(eq(1L), eq(EpisodeStatus.SCHEDULED), any(Pageable.class))).thenReturn(episodePage);
        when(content.getDtype()).thenReturn("WEBNOVEL");
        when(content.getTitle()).thenReturn("제목");
        when(content.getEpisodeCount()).thenReturn(20);
        when(episodeProvider.getGroupByStats(eq(1L))).thenReturn(List.of());

        //when
        EpisodeDashboard result = creatorEpisodeService.getEpisodeDashboard(1L, 1L, PageRequest.of(0, 10), "SCHEDULED", "latest");

        // then
        assertNotNull(result);
        assertEquals("제목", result.getContentTitle());
        assertEquals("webnovels", result.getContentType());
        verify(episodeProvider, never()).getAllEpisodesByContent(eq(1L), any(Pageable.class));

    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void getEpisodeDashboard_whenCreatorNotFound_shouldThrowException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.getEpisodeDashboard(1L, 1L, PageRequest.of(0, 10), "ALL", "latest"));

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());
        verify(episodeProvider, never()).getAllEpisodesByContent(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("콘텐츠가 없으면 CustomException 발생")
    void getEpisodeDashboard_whenContentNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(eq(1L), eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.getEpisodeDashboard(1L, 1L, PageRequest.of(0, 10), "ALL", "latest")
        );

        // then
        assertEquals(ErrorCode.CONTENT_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 콘텐츠입니다.", exception.getErrorMessage());
        verify(episodeProvider, never()).getAllEpisodesByContent(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("웹소설 에피소드 상세 조회 성공")
    void getWebnovelEpisodeDetail_withValidInput_shouldReturnDetail() {

        Creator creator = Creator.builder().id(1L).build();
        WebnovelEpisode episode = mockWebnovelEpisode(creator);

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        //when
        WebnovelEpisodeDetail result = creatorEpisodeService.getWebnovelEpisodeDetail(1L, 1L);

        // then
        assertNotNull(result);
        verify(webnovelEpisodeRepository).findById(eq(1L));
        assertEquals("웹소설 제목", result.getContentTitle());
        assertEquals("에피소드 내용", result.getContent());

    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void getWebnovelEpisodeDetail_whenCreatorNotFound_shouldThrowException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.getWebnovelEpisodeDetail(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("웹소설 에피소드가 없으면 CustomException 발생")
    void getWebnovelEpisodeDetail_whenEpisodeNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelEpisodeRepository.findById(eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.getWebnovelEpisodeDetail(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.EPISODE_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("해당 에피소드를 찾을 수 없습니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("다른 크리에이터의 웹소설 에피소드 조회 시 CustomException 발생")
    void getWebnovelEpisodeDetail_whenNotOwner_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Creator anotherCreator = Creator.builder().id(10L).build();
        WebnovelEpisode episode = mockWebnovelEpisode(anotherCreator);


        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.getWebnovelEpisodeDetail(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.NOT_CONTENT_OWNER, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("콘텐츠 작성자가 아닙니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("웹툰 에피소드 상세 조회 성공")
    void getWebtoonEpisodeDetail_withValidInput_shouldReturnDetail() {

        Creator creator = Creator.builder().id(1L).build();
        WebtoonEpisode episode = mockWebtoonEpisode(creator);
        List<EpisodeImage> images = List.of();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));
        when(webtoonImageService.getWebtoonImages(eq(episode.getImages()))).thenReturn(images);
        //when
        WebtoonEpisodeDetail result = creatorEpisodeService.getWebtoonEpisodeDetail(1L, 1L);

        // then
        assertNotNull(result);
        verify(webtoonEpisodeRepository).findById(eq(1L));
        assertEquals("웹툰 제목", result.getContentTitle());
        assertEquals(0, result.getEpisodeImages().size());
    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void getWebtoonEpisodeDetail_whenCreatorNotFound_shouldThrowException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.getWebtoonEpisodeDetail(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("웹툰 에피소드가 없으면 CustomException 발생")
    void getWebtoonEpisodeDetail_whenEpisodeNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonEpisodeRepository.findById(eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.getWebtoonEpisodeDetail(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.EPISODE_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("해당 에피소드를 찾을 수 없습니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("다른 크리에이터의 웹툰 에피소드 조회 시 CustomException 발생")
    void getWebtoonEpisodeDetail_whenNotOwner_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Creator anotherCreator = Creator.builder().id(10L).build();
        WebtoonEpisode episode = mockWebtoonEpisode(anotherCreator);


        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.getWebtoonEpisodeDetail(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.NOT_CONTENT_OWNER, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("콘텐츠 작성자가 아닙니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("웹소설 에피소드 수정 성공")
    void updateWebnovelEpisode_withValidInput_shouldUpdateEpisode() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        WebnovelEpisode episode = mockWebnovelEpisode(creator);
        WebnovelEpisodeUpdate request = new WebnovelEpisodeUpdate("수정된 제목", LocalDate.now().plusDays(4), "수정 내용");

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        //when
        Long result = creatorEpisodeService.updateWebnovelEpisode(1L, 1L, request);

        // then
        assertEquals(1L, result);
        verify(episode).updateEpisode(eq("수정된 제목"), any());
        verify(episode).updateContent(eq("수정 내용"));
    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void updateWebnovelEpisode_whenCreatorNotFound_shouldThrowException() {
        // given
        WebnovelEpisodeUpdate request = new WebnovelEpisodeUpdate("수정된 제목", LocalDate.now().plusDays(4), "수정 내용");
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.updateWebnovelEpisode(1L, 1L, request)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

    }



    @Test
    @DisplayName("웹소설 에피소드가 없으면 CustomException 발생")
    void updateWebnovelEpisode_whenEpisodeNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        WebnovelEpisodeUpdate request = new WebnovelEpisodeUpdate("수정된 제목", LocalDate.now().plusDays(4), "수정 내용");
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelEpisodeRepository.findById(eq(1L))).thenReturn(Optional.empty());


        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.updateWebnovelEpisode(1L, 1L, request)
        );

        // then
        assertEquals(ErrorCode.EPISODE_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("해당 에피소드를 찾을 수 없습니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("다른 크리에이터의 웹소설 에피소드 조회 시 CustomException 발생")
    void updateWebnovelEpisode_whenNotOwner_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Creator anotherCreator = Creator.builder().id(10L).build();
        WebnovelEpisode episode = mockWebnovelEpisode(anotherCreator);
        WebnovelEpisodeUpdate request = new WebnovelEpisodeUpdate("수정된 제목", LocalDate.now().plusDays(4), "수정 내용");

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.updateWebnovelEpisode(1L, 1L, request)
        );

        // then
        assertEquals(ErrorCode.NOT_CONTENT_OWNER, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("콘텐츠 작성자가 아닙니다.", exception.getErrorMessage());
        verify(episode, never()).updateEpisode(any(), any());
        verify(episode, never()).updateContent(any());
    }

    @Test
    @DisplayName("웹툰 에피소드 수정 성공")
    void updateWebtoonEpisode_withValidInput_shouldUpdateEpisode() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        WebtoonEpisode episode = mockWebtoonEpisode(creator);
        List<WebtoonEpisodeUpdate.ExistingImage> existingImages = List.of(
                mock(WebtoonEpisodeUpdate.ExistingImage.class)
        );

        WebtoonEpisodeUpdate request = new WebtoonEpisodeUpdate("수정 제목", LocalDate.now().plusDays(1), existingImages, List.of(1, 2));
        MultipartFile[] newFiles = {mock(MultipartFile.class)};

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        //when
        Long result = creatorEpisodeService.updateWebtoonEpisode(1L, 1L, request, newFiles);

        // then
        assertEquals(1L, result);
        verify(episode).updateEpisode(eq("수정 제목"), any());
        verify(webtoonImageService).updateWebtoonImages(eq(episode), eq(request), eq(newFiles));

    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void updateWebtoonEpisode_whenCreatorNotFound_shouldThrowException() {
        // given
        List<WebtoonEpisodeUpdate.ExistingImage> existingImages = List.of(
                mock(WebtoonEpisodeUpdate.ExistingImage.class)
        );
        WebtoonEpisodeUpdate request = new WebtoonEpisodeUpdate("수정 제목", LocalDate.now().plusDays(1), existingImages, List.of(1, 2));
        MultipartFile[] newFiles = {mock(MultipartFile.class)};
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.updateWebtoonEpisode(1L, 1L, request, newFiles)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("웹툰 에피소드가 없으면 CustomException 발생")
    void updateWebtoonEpisode_whenEpisodeNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        List<WebtoonEpisodeUpdate.ExistingImage> existingImages = List.of(
                mock(WebtoonEpisodeUpdate.ExistingImage.class)
        );
        WebtoonEpisodeUpdate request = new WebtoonEpisodeUpdate("수정 제목", LocalDate.now().plusDays(1), existingImages, List.of(1, 2));
        MultipartFile[] newFiles = {mock(MultipartFile.class)};

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonEpisodeRepository.findById(eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.updateWebtoonEpisode(1L, 1L, request, newFiles)
        );

        // then
        assertEquals(ErrorCode.EPISODE_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("해당 에피소드를 찾을 수 없습니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("다른 크리에이터의 웹툰 에피소드 조회 시 CustomException 발생")
    void updateWebtoonEpisode_whenNotOwner_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Creator anotherCreator = Creator.builder().id(10L).build();
        WebtoonEpisode episode = mockWebtoonEpisode(anotherCreator);

        List<WebtoonEpisodeUpdate.ExistingImage> existingImages = List.of(
                mock(WebtoonEpisodeUpdate.ExistingImage.class)
        );
        WebtoonEpisodeUpdate request = new WebtoonEpisodeUpdate("수정 제목", LocalDate.now().plusDays(1), existingImages, List.of(1, 2));
        MultipartFile[] newFiles = {mock(MultipartFile.class)};


        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.updateWebtoonEpisode(1L, 1L, request, newFiles)
        );

        // then
        assertEquals(ErrorCode.NOT_CONTENT_OWNER, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("콘텐츠 작성자가 아닙니다.", exception.getErrorMessage());
        verify(episode, never()).updateEpisode(any(), any());
    }

    @Test
    @DisplayName("웹소설 에피소드 삭제 성공")
    void deleteWebnovelEpisode_withValidUserId_shouldDeleteEpisode() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        WebnovelEpisode episode = mockWebnovelEpisode(creator);

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        //when
        creatorEpisodeService.deleteWebnovelEpisode(1L, 1L);

        // then
        verify(episode).deleteEpisode();

    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void deleteWebnovelEpisode_whenCreatorNotFound_shouldThrowException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.deleteWebnovelEpisode(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("웹소설 에피소드가 없으면 CustomException 발생")
    void deleteWebnovelEpisode_whenEpisodeNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelEpisodeRepository.findById(eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.deleteWebnovelEpisode(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.EPISODE_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("해당 에피소드를 찾을 수 없습니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("다른 크리에이터의 웹소설 에피소드 조회 시 CustomException 발생")
    void deleteWebnovelEpisode_whenNotOwner_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Creator anotherCreator = Creator.builder().id(10L).build();
        WebnovelEpisode episode = mockWebnovelEpisode(anotherCreator);


        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webnovelEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.deleteWebnovelEpisode(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.NOT_CONTENT_OWNER, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("콘텐츠 작성자가 아닙니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("웹툰 에피소드 삭제 성공")
    void deleteWebtoonEpisode_withValidUserId_shouldDeleteEpisode() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        WebtoonEpisode episode = mockWebtoonEpisode(creator);

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        //when
        creatorEpisodeService.deleteWebtoonEpisode(1L, 1L);

        // then
        verify(episode).deleteEpisode();
        verify(webtoonImageService).deleteWebtoonImages(episode);

    }

    @Test
    @DisplayName("크리에이터가 없으면 CustomException 발생")
    void deleteWebtoonEpisode_whenCreatorNotFound_shouldThrowException() {
        // given
        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.deleteWebtoonEpisode(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.CREATOR_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 작가입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("웹툰 에피소드가 없으면 CustomException 발생")
    void deleteWebtoonEpisode_whenEpisodeNotFound_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();

        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonEpisodeRepository.findById(eq(1L))).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.deleteWebtoonEpisode(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.EPISODE_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("해당 에피소드를 찾을 수 없습니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("다른 크리에이터의 웹툰 에피소드 조회 시 CustomException 발생")
    void deleteWebtoonEpisode_whenNotOwner_shouldThrowCustomException() {
        // given
        Creator creator = Creator.builder().id(1L).build();
        Creator anotherCreator = Creator.builder().id(10L).build();
        WebtoonEpisode episode = mockWebtoonEpisode(anotherCreator);


        when(creatorRepository.findByUser_Id(1L)).thenReturn(Optional.of(creator));
        when(webtoonEpisodeRepository.findById(eq(1L))).thenReturn(Optional.of(episode));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> creatorEpisodeService.deleteWebtoonEpisode(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.NOT_CONTENT_OWNER, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("콘텐츠 작성자가 아닙니다.", exception.getErrorMessage());
        verify(episode, never()).updateEpisode(any(), any());
    }


}