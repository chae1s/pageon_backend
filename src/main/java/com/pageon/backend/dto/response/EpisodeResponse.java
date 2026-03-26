package com.pageon.backend.dto.response;

import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.entity.*;
import com.pageon.backend.entity.base.EpisodeBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class EpisodeResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private Integer episodeNum;
        private String episodeTitle;
        private LocalDateTime createdAt;
        private Integer purchasePrice;
        private Integer rentalPrice;
        private EpisodeResponse.Purchase episodePurchase;

        public static Summary fromEntity(EpisodeBase episode, EpisodeResponse.Purchase purchase) {
            return Summary.builder()
                    .id(episode.getId())
                    .episodeNum(episode.getEpisodeNum())
                    .episodeTitle(episode.getEpisodeTitle())
                    .createdAt(episode.getCreatedAt())
                    .purchasePrice(episode.getPurchasePrice())
                    .rentalPrice(episode.getRentalPrice())
                    .episodePurchase(purchase)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Purchase {
        private Long id;
        private PurchaseType purchaseType;
        private LocalDateTime expiredAt;

        public static Purchase fromEntity(EpisodePurchase episodePurchase) {
            return Purchase.builder()
                    .id(episodePurchase.getId())
                    .purchaseType(episodePurchase.getPurchaseType())
                    .expiredAt(episodePurchase.getExpiredAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebnovelDetail {
        private Long id;
        private String title;
        private Integer episodeNum;
        private String episodeTitle;
        private String content;
        private Integer purchasePrice;
        private Double averageRating;
        private Long ratingCount;
        private Long prevEpisodeId;
        private Long nextEpisodeId;
        private Integer userScore;
        private CommentResponse.Best bestComment;

        public static WebnovelDetail fromEntity(
                WebnovelEpisode webnovelEpisode, String title,
                Long prevEpisodeId, Long nextEpisodeId, Integer userScore, CommentResponse.Best bestComment)
        {
            return WebnovelDetail.builder()
                    .id(webnovelEpisode.getId())
                    .title(title)
                    .episodeNum(webnovelEpisode.getEpisodeNum())
                    .episodeTitle(webnovelEpisode.getEpisodeTitle())
                    .content(webnovelEpisode.getContent())
                    .purchasePrice(webnovelEpisode.getPurchasePrice())
                    .averageRating(webnovelEpisode.getAverageRating())
                    .ratingCount(webnovelEpisode.getRatingCount())
                    .prevEpisodeId(prevEpisodeId)
                    .nextEpisodeId(nextEpisodeId)
                    .userScore(userScore)
                    .bestComment(bestComment)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebtoonDetail {
        private Long id;
        private String title;
        private String episodeTitle;
        private Integer episodeNum;
        private Integer purchasePrice;
        private Integer rentalPrice;
        private Double averageRating;
        private Long ratingCount;
        private List<EpisodeResponse.EpisodeImage> images;
        private Long prevEpisodeId;
        private Long nextEpisodeId;
        private Integer userScore;
        private CommentResponse.Best bestComment;

        public static WebtoonDetail fromEntity(
                WebtoonEpisode episode, String title, List<EpisodeResponse.EpisodeImage> images,
                Long prevEpisodeId, Long nextEpisodeId, Integer userScore, CommentResponse.Best bestComment)
        {
            return WebtoonDetail.builder()
                    .id(episode.getId())
                    .title(title)
                    .episodeTitle(episode.getEpisodeTitle())
                    .episodeNum(episode.getEpisodeNum())
                    .purchasePrice(episode.getPurchasePrice())
                    .rentalPrice(episode.getRentalPrice())
                    .averageRating(episode.getAverageRating())
                    .ratingCount(episode.getRatingCount())
                    .images(images)
                    .prevEpisodeId(prevEpisodeId)
                    .nextEpisodeId(nextEpisodeId)
                    .userScore(userScore)
                    .bestComment(bestComment)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EpisodeImage {
        private Long id;
        private Integer sequence;
        private String imageUrl;

        public static EpisodeImage fromEntity(WebtoonImage webtoonImage, String signUrl) {

            return EpisodeImage.builder()
                    .id(webtoonImage.getId())
                    .sequence(webtoonImage.getSequence())
                    .imageUrl(signUrl)
                    .build();
        }
    }


}
