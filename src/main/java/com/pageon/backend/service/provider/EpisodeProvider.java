package com.pageon.backend.service.provider;

import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.dto.response.CommentResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.entity.base.EpisodeCommentBase;
import com.pageon.backend.service.EpisodePurchaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EpisodeProvider {
    boolean supports(String contentType);

    Object findEpisodeDetail(Long userId, Long episodeId);

    // 에피소드 평점
    void rateEpisode(User user, Long episodeId, Integer score);

    void updateEpisodeRating(Long userId, Long episodeId, Integer newScore);

    // 에피소드 댓글
    void saveComment(User user, Long episodeId, String text, Boolean isSpoiler);
    void updateComment(Long userId, Long commentId, String text, Boolean isSpoiler);
    void deleteComment(Long userId, Long commentId);

    Page<? extends EpisodeCommentBase> findComments(Long episodeId, Pageable pageable);
    Page<? extends EpisodeCommentBase> findMyComments(Long userId, Pageable pageable);

    CommentResponse.Best findBestComment(Long episodeId);
    Set<Long> getLikedCommentIds(Long userId, List<Long> commentIds);

    Boolean hasLiked(Long userId, Long commentId);
    void saveLike(User user, Long commentId);
    void deleteLike(Long userId, Long commentId);
    EpisodePurchaseService.EpisodeInfo getEpisodeInfo(Long episodeId, PurchaseType purchaseType);

    Optional<Integer> getMaxEpisodeNum(Long contentId);
}
