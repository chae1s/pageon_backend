package com.pageon.backend.service;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.dto.response.EpisodeResponse;
import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.Webtoon;
import com.pageon.backend.entity.WebtoonEpisode;
import com.pageon.backend.entity.WebtoonImage;
import com.pageon.backend.repository.WebtoonImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Transactional
@ActiveProfiles("test")
@DisplayName("WebtoonImageService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WebtoonImageServiceTest {
    @InjectMocks
    private WebtoonImageService webtoonImageService;
    @Mock
    private WebtoonImageRepository webtoonImageRepository;

    @BeforeEach
    void setUp() {
        webtoonImageRepository.deleteAll();
    }

    @Test
    @DisplayName("webtoonEpisodeId로 image 리스트를 return")
    void getWebtoonImagesByEpisodeId_withWebtoonEpisodeId_shouldReturnWebtoonImages() {

        // given
        List<WebtoonImage> webtoonImages = createMockWebtoonImages();

        when(webtoonImageRepository.findByWebtoonEpisodeIdOrderBySequenceAsc(1L)).thenReturn(webtoonImages);

        //when
        List<EpisodeResponse.EpisodeImage> result = webtoonImageService.getWebtoonImages(1L);

        // then
        assertEquals(webtoonImages.size(), result.size());
        assertEquals(webtoonImages.get(0).getSequence(), result.get(0).getSequence());

    }

    private List<WebtoonImage> createMockWebtoonImages() {
        Creator creator = Creator.builder()
                .id(1L)
                .penName("필명")
                .agreedToAiPolicy(true)
                .aiPolicyAgreedAt(LocalDateTime.now())
                .build();

        Webtoon webtoon = Webtoon.builder()
                .id(1L)
                .title("테스트")
                .description("테스트")
                .creator(creator)
                .serialDay(SerialDay.MONDAY)
                .status(SeriesStatus.ONGOING)
                .build();

        WebtoonEpisode webtoonEpisode = WebtoonEpisode.builder()
                .id(1L)
                .webtoon(webtoon)
                .episodeNum(1)
                .episodeTitle("웹툰")
                .rentalPrice(100)
                .purchasePrice(300)
                .build();

        List<WebtoonImage> images = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            images.add(
                    new WebtoonImage((long) i, i, "url" + i, webtoonEpisode)
            );
        }

        return images;
    }

}
