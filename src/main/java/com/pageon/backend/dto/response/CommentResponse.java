package com.pageon.backend.dto.response;

import com.pageon.backend.entity.WebnovelEpisodeComment;
import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.entity.base.EpisodeCommentBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CommentResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Best {
        private Long id;
        private String text;
        private String nickname;
        private LocalDateTime createdAt;
        private Long totalCount;

        public static Best fromEntity(EpisodeCommentBase comment, Long totalCount) {
            if (comment == null) {
                return Best.builder()
                        .totalCount(totalCount)
                        .build();
            }

            return Best.builder()
                    .id(comment.getId())
                    .text(comment.getText())
                    .nickname(comment.getUser().getNickname())
                    .createdAt(comment.getCreatedAt())
                    .totalCount(totalCount)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyComment {
        private Long id;
        private String text;
        private Long episodeId;
        private Integer episodeNum;
        private Long contentId;
        private String contentTitle;
        private LocalDateTime createdAt;
        private Long likeCount;

        public static MyComment fromEntity(EpisodeCommentBase comment, EpisodeBase episode) {
            return MyComment.builder()
                    .id(comment.getId())
                    .text(comment.getText())
                    .episodeId(episode.getId())
                    .episodeNum(episode.getEpisodeNum())
                    .contentId(episode.getParentContent().getId())
                    .contentTitle(episode.getParentContent().getTitle())
                    .createdAt(comment.getCreatedAt())
                    .likeCount(comment.getLikeCount())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String text;
        private Integer episodeNum;
        private String contentTitle;
        private String nickname;
        private LocalDateTime createdAt;
        private Boolean isSpoiler;
        private Boolean isMine;
        private Long likeCount;
        private Boolean isLiked;

        public static Summary fromEntity(EpisodeCommentBase comment, Long currentUserId, EpisodeBase episode, Boolean isLiked) {
            Boolean isMine = (comment.getUser().getId().equals(currentUserId));

            return Summary.builder()
                    .id(comment.getId())
                    .text(comment.getText())
                    .episodeNum(episode.getEpisodeNum())
                    .contentTitle(episode.getParentContent().getTitle())
                    .nickname(comment.getUser().getNickname())
                    .createdAt(comment.getCreatedAt())
                    .isSpoiler(comment.getIsSpoiler())
                    .isMine(isMine)
                    .likeCount(comment.getLikeCount())
                    .isLiked(isLiked)
                    .build();
        }
    }
}
