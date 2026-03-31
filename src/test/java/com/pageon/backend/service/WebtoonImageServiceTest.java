package com.pageon.backend.service;

import com.pageon.backend.dto.request.episode.WebtoonEpisodeUpdate;
import com.pageon.backend.dto.response.episode.EpisodeImage;
import com.pageon.backend.entity.Webtoon;
import com.pageon.backend.entity.WebtoonEpisode;
import com.pageon.backend.entity.WebtoonImage;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.WebtoonImageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ActiveProfiles("test")
@DisplayName("WebtoonImageService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WebtoonImageServiceTest {
    @InjectMocks
    private WebtoonImageService webtoonImageService;
    @Mock
    private WebtoonImageRepository webtoonImageRepository;
    @Mock
    private FileUploadService fileUploadService;
    @Mock
    private CloudFrontSignerService cloudFrontSignerService;

    private WebtoonEpisode mockWebtoonEpisode(List<WebtoonImage> images) {
        WebtoonEpisode episode = mock(WebtoonEpisode.class);
        Webtoon webtoon = mock(Webtoon.class);
        lenient().when(webtoon.getId()).thenReturn(1L);
        lenient().when(episode.getWebtoon()).thenReturn(webtoon);
        lenient().when(episode.getEpisodeNum()).thenReturn(10);
        lenient().when(episode.getImages()).thenReturn(images);

        return episode;
    }

    @Test
    @DisplayName("웹툰 이미지 등록 성공")
    void registerWebtoonImage_withValidInput_shouldAddImage() {
        // given
        List<WebtoonImage> images = new ArrayList<>();
        WebtoonEpisode episode = mockWebtoonEpisode(images);

        MultipartFile[] files = {
                mock(MultipartFile.class),
                mock(MultipartFile.class)
        };

        List<String> s3Urls = List.of("https://s3.url/img1.jpg", "https://s3.url/img2.jpg");

        when(fileUploadService.uploadMultiple(any(MultipartFile[].class), anyString())).thenReturn(s3Urls);

        //when
        webtoonImageService.registerWebtoonImage(1L, episode, files);

        // then
        assertEquals(2, images.size());
        assertEquals(1, images.get(0).getSequence());
        assertEquals("https://s3.url/img1.jpg", images.get(0).getImageUrl());
        assertEquals(2, images.get(1).getSequence());
        assertEquals("https://s3.url/img2.jpg", images.get(1).getImageUrl());
        assertEquals(episode, images.get(0).getWebtoonEpisode());

    }

    @Test
    @DisplayName("파일이 없으면 이미지가 추가되지 않는다")
    void registerWebtoonImage_withEmptyFiles_shouldNotAddImages() {
        // given
        List<WebtoonImage> images = new ArrayList<>();
        WebtoonEpisode episode = mockWebtoonEpisode(images);

        when(fileUploadService.uploadMultiple(
                any(MultipartFile[].class), anyString()))
                .thenReturn(List.of());

        // when
        webtoonImageService.registerWebtoonImage(1L, episode, new MultipartFile[0]);

        // then
        assertTrue(images.isEmpty());
    }

    @Test
    @DisplayName("웹툰 에피소드 목록 조회 성공")
    void getWebtoonImages_withValidEpisodeId_shouldReturnImages() {
        // given
        WebtoonImage images1 = mock(WebtoonImage.class);
        when(images1.getImageUrl()).thenReturn("https://s3.url/img1.jpg");
        when(images1.getSequence()).thenReturn(1);
        WebtoonImage images2 = mock(WebtoonImage.class);
        when(images2.getImageUrl()).thenReturn("https://s3.url/img2.jpg");
        when(images2.getSequence()).thenReturn(2);

        List<WebtoonImage> images = List.of(images1, images2);
        when(cloudFrontSignerService.signUrl("https://s3.url/img1.jpg"))
                .thenReturn("https://cdn.signed/img1.jpg");
        when(cloudFrontSignerService.signUrl("https://s3.url/img2.jpg"))
                .thenReturn("https://cdn.signed/img2.jpg");

        //when
        List<EpisodeImage> result = webtoonImageService.getWebtoonImages(images);

        // then
        assertEquals(2, result.size());
        assertEquals("https://cdn.signed/img1.jpg", result.get(0).getImageUrl());
        assertEquals("https://cdn.signed/img2.jpg", result.get(1).getImageUrl());
        verify(cloudFrontSignerService, times(2)).signUrl(anyString());


    }

    @Test
    @DisplayName("이미지가 없으면 빈 리스트 반환")
    void getWebtoonImages_withEmptyList_shouldReturnEmptyList() {
        // when
        List<EpisodeImage> result = webtoonImageService.getWebtoonImages(List.of());

        // then
        assertTrue(result.isEmpty());
        verify(cloudFrontSignerService, never()).signUrl(any());
    }

    @Test
    @DisplayName("웹툰 이미지 수정 성공 - 기존 이미지 유지 + 새 이미지 추가")
    void updateWebtoonImages_withNewAndExistingImages_shouldUpdateImages() {
        // given
        WebtoonImage keepImage = mock(WebtoonImage.class);
        when(keepImage.getId()).thenReturn(1L);

        WebtoonImage deleteImage = mock(WebtoonImage.class);
        when(deleteImage.getId()).thenReturn(2L);
        when(deleteImage.getImageUrl()).thenReturn("https://s3.url/delete.jpg");

        List<WebtoonImage> currentImages = new ArrayList<>(List.of(keepImage, deleteImage));
        WebtoonEpisode episode = mockWebtoonEpisode(currentImages);

        WebtoonEpisodeUpdate request = new WebtoonEpisodeUpdate(
                "제목", LocalDate.now(),
                List.of(WebtoonEpisodeUpdate.ExistingImage.builder().id(1L).sequence(1).build()),
                List.of(2)
        );

        MultipartFile[] newFiles = {mock(MultipartFile.class)};

        when(webtoonImageRepository.findById(1L)).thenReturn(Optional.of(keepImage));
        when(fileUploadService.uploadMultiple(any(MultipartFile[].class), anyString()))
                .thenReturn(List.of("https://s3.url/new.jpg"));

        // when
        webtoonImageService.updateWebtoonImages(episode, request, newFiles);

        // then
        verify(fileUploadService).deleteFile("https://s3.url/delete.jpg");
        verify(webtoonImageRepository).delete(deleteImage);
        verify(keepImage).updateSequence(1);
        verify(fileUploadService).uploadMultiple(
                any(MultipartFile[].class), eq("webtoons/1/episode/10"));
    }

    @Test
    @DisplayName("웹툰 이미지 수정 성공 - 새 이미지 없음")
    void updateWebtoonImages_withOnlyExistingImages_shouldNotUpload() {
        // given
        WebtoonImage keepImage = mock(WebtoonImage.class);
        when(keepImage.getId()).thenReturn(1L);

        List<WebtoonImage> currentImages = new ArrayList<>(List.of(keepImage));
        WebtoonEpisode episode = mockWebtoonEpisode(currentImages);

        WebtoonEpisodeUpdate request = new WebtoonEpisodeUpdate(
                "제목", LocalDate.now(),
                List.of(WebtoonEpisodeUpdate.ExistingImage.builder().id(1L).sequence(1).build()),
                List.of()
        );

        when(webtoonImageRepository.findById(1L)).thenReturn(Optional.of(keepImage));

        // when
        webtoonImageService.updateWebtoonImages(episode, request, null);

        // then
        verify(fileUploadService, never()).deleteFile(any());
        verify(webtoonImageRepository, never()).delete(any());
        verify(fileUploadService, never()).uploadMultiple(any(), any());
        verify(keepImage).updateSequence(1);
    }

    @Test
    @DisplayName("웹툰 이미지 수정 성공 - 모든 이미지 교체")
    void updateWebtoonImages_withAllNewImages_shouldDeleteAllAndUpload() {
        // given
        WebtoonImage deleteImage = mock(WebtoonImage.class);
        when(deleteImage.getId()).thenReturn(1L);
        when(deleteImage.getImageUrl()).thenReturn("https://s3.url/old.jpg");

        List<WebtoonImage> currentImages = new ArrayList<>(List.of(deleteImage));
        WebtoonEpisode episode = mockWebtoonEpisode(currentImages);

        MultipartFile[] newFiles = {mock(MultipartFile.class)};
        WebtoonEpisodeUpdate request = new WebtoonEpisodeUpdate(
                "제목", LocalDate.now(),
                List.of(), // 기존 이미지 없음
                List.of(1)
        );

        when(fileUploadService.uploadMultiple(any(MultipartFile[].class), anyString()))
                .thenReturn(List.of("https://s3.url/new.jpg"));

        // when
        webtoonImageService.updateWebtoonImages(episode, request, newFiles);

        // then
        verify(fileUploadService).deleteFile("https://s3.url/old.jpg");
        verify(webtoonImageRepository).delete(deleteImage);
    }

    @Test
    @DisplayName("존재하지 않는 이미지 ID면 CustomException 발생")
    void updateWebtoonImages_whenImageNotFound_shouldThrowException() {
        // given
        WebtoonImage keepImage = mock(WebtoonImage.class);
        when(keepImage.getId()).thenReturn(1L);

        List<WebtoonImage> currentImages = new ArrayList<>(List.of(keepImage));
        WebtoonEpisode episode = mockWebtoonEpisode(currentImages);

        WebtoonEpisodeUpdate request = new WebtoonEpisodeUpdate(
                "제목", LocalDate.now(),
                List.of(WebtoonEpisodeUpdate.ExistingImage.builder().id(99L).sequence(1).build()),
                List.of()
        );

        when(webtoonImageRepository.findById(99L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> webtoonImageService.updateWebtoonImages(episode, request, null));

        // then
        assertEquals(ErrorCode.IMAGE_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("웹툰 에피소드 이미지를 찾을 수 없습니다.", exception.getErrorMessage());
    }

    @Test
    @DisplayName("웹툰 이미지 삭제 성공")
    void deleteWebtoonImages_withValidEpisode_shouldDeleteAllImages() {
        // given
        WebtoonImage image1 = mock(WebtoonImage.class);
        when(image1.getImageUrl()).thenReturn("https://s3.url/img1.jpg");

        WebtoonImage image2 = mock(WebtoonImage.class);
        when(image2.getImageUrl()).thenReturn("https://s3.url/img2.jpg");

        List<WebtoonImage> images = List.of(image1, image2);
        WebtoonEpisode episode = mockWebtoonEpisode(new ArrayList<>(images));

        // when
        webtoonImageService.deleteWebtoonImages(episode);

        // then
        verify(fileUploadService).deleteFile("https://s3.url/img1.jpg");
        verify(fileUploadService).deleteFile("https://s3.url/img2.jpg");
        verify(webtoonImageRepository).deleteAll(images);
    }

    @Test
    @DisplayName("이미지가 없으면 삭제 시도 안 함")
    void deleteWebtoonImages_withNoImages_shouldNotCallDelete() {
        // given
        WebtoonEpisode episode = mockWebtoonEpisode(new ArrayList<>());

        // when
        webtoonImageService.deleteWebtoonImages(episode);

        // then
        verify(fileUploadService, never()).deleteFile(any());
        verify(webtoonImageRepository).deleteAll(List.of());
    }




}
