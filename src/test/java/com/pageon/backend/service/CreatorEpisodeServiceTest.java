package com.pageon.backend.service;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.request.episode.WebnovelEpisodeCreate;
import com.pageon.backend.dto.request.episode.WebtoonEpisodeCreate;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;

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

}