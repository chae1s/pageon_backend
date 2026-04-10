package com.pageon.backend.service.provider;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.dto.response.CommentResponse;
import com.pageon.backend.dto.response.EpisodeResponse;
import com.pageon.backend.dto.response.creator.episode.EpisodeList;
import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import com.pageon.backend.entity.*;
import com.pageon.backend.entity.base.EpisodeCommentBase;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import com.pageon.backend.repository.episode.WebnovelEpisodeRepository;
import com.pageon.backend.service.EpisodePurchaseService;
import com.pageon.backend.service.handler.EpisodeActionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WebnovelEpisodeProvider implements EpisodeProvider {

    private final WebnovelEpisodeRepository webnovelEpisodeRepository;
    private final EpisodeActionHandler actionHandler;
    private final WebnovelEpisodeRatingRepository webnovelEpisodeRatingRepository;
    private final WebnovelEpisodeCommentRepository webnovelEpisodeCommentRepository;
    private final WebnovelEpisodeCommentLikeRepository webnovelEpisodeCommentLikeRepository;

    @Override
    public boolean supports(String contentType) {
        return "webnovels".equals(contentType);
    }

    @Override
    public Page<EpisodeSummaryResponse> findEpisodeSummaries(Long contentId, String sort, Pageable pageable) {
        return webnovelEpisodeRepository.findEpisodeSummaries(contentId, sort, pageable);
    }

    @Override
    @Transactional
    public EpisodeResponse.WebnovelDetail findEpisodeDetail(Long userId, Long episodeId) {

        WebnovelEpisode episode = webnovelEpisodeRepository.findWithWebnovelById(episodeId).orElseThrow(
                () -> new CustomException(ErrorCode.EPISODE_NOT_FOUND)
        );

        actionHandler.handleViewEffects(userId, episode.getWebnovel(), episode, ContentType.WEBNOVEL);

        Long prevId = webnovelEpisodeRepository.findPrevEpisodeId(episode.getWebnovel().getId(), episode.getEpisodeNum());
        Long nextId = webnovelEpisodeRepository.findNextEpisodeId(episode.getWebnovel().getId(), episode.getEpisodeNum());

        Integer userScore = webnovelEpisodeRatingRepository.findScoreByWebnovelEpisodeAndUser(episodeId, userId);

        return EpisodeResponse.WebnovelDetail.fromEntity(
                episode, episode.getWebnovel().getTitle(), prevId, nextId, userScore, findBestComment(episodeId)
        );
    }

    @Override
    public void rateEpisode(User user, Long episodeId, Integer score) {

        WebnovelEpisode episode = getWebnovelEpisode(episodeId);

        WebnovelEpisodeRating rating = WebnovelEpisodeRating.builder()
                .user(user)
                .webnovelEpisode(episode)
                .score(score)
                .build();

        webnovelEpisodeRatingRepository.save(rating);

        actionHandler.handleSaveRate(user.getId(), episode.getWebnovel(), ContentType.WEBNOVEL, episode, score);

    }

    @Override
    public void updateEpisodeRating(Long userId, Long episodeId, Integer newScore) {
        WebnovelEpisode episode = getWebnovelEpisode(episodeId);

        WebnovelEpisodeRating rating = webnovelEpisodeRatingRepository.findByWebnovelEpisodeAndUser(episodeId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.EPISODE_RATING_NOT_FOUND)
        );

        Integer oldScore = rating.getScore();

        actionHandler.handleUpdateRate(episode.getWebnovel(), episode, rating, newScore, oldScore);
    }

    private WebnovelEpisode getWebnovelEpisode(Long episodeId) {
        WebnovelEpisode webnovelEpisode = webnovelEpisodeRepository.findWithWebnovelById(episodeId).orElseThrow(
                () -> new CustomException(ErrorCode.EPISODE_NOT_FOUND)
        );

        if (webnovelEpisode.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.EPISODE_IS_DELETED);
        }

        return webnovelEpisode;
    }

    @Override
    public void saveComment(User user, Long episodeId, String text, Boolean isSpoiler) {

        WebnovelEpisode episode = getWebnovelEpisode(episodeId);

        WebnovelEpisodeComment comment = WebnovelEpisodeComment.builder()
                .user(user)
                .webnovelEpisode(episode)
                .text(text)
                .isSpoiler(isSpoiler)
                .build();

        webnovelEpisodeCommentRepository.save(comment);

        actionHandler.handleSaveComment(user.getId(), episode.getWebnovel(), ContentType.WEBNOVEL);
    }

    @Override
    public void updateComment(Long userId, Long commentId, String text, Boolean isSpoiler) {
        WebnovelEpisodeComment comment = webnovelEpisodeCommentRepository.findByIdAndUser_Id(userId, commentId).orElseThrow(
                () -> new CustomException(ErrorCode.COMMENT_NOT_FOUND)
        );

        comment.updateComment(text, isSpoiler);
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        WebnovelEpisodeComment comment = webnovelEpisodeCommentRepository.findByIdAndUser_Id(userId, commentId).orElseThrow(
                () -> new CustomException(ErrorCode.COMMENT_NOT_FOUND)
        );

        if (comment.getDeletedAt() != null) throw new CustomException(ErrorCode.COMMENT_ALREADY_DELETED);

        comment.deleteComment(LocalDateTime.now());
    }

    @Override
    public Page<? extends EpisodeCommentBase> findComments(Long episodeId, Pageable pageable) {
        return webnovelEpisodeCommentRepository.findAllByWebnovelEpisode_IdAndDeletedAtNull(episodeId, pageable);
    }

    @Override
    public Page<? extends EpisodeCommentBase> findMyComments(Long userId, Pageable pageable) {
        return webnovelEpisodeCommentRepository.findAllByUser_IdAndDeletedAtNull(userId, pageable);
    }

    @Override
    public CommentResponse.Best findBestComment(Long episodeId) {
        WebnovelEpisodeComment comment = webnovelEpisodeCommentRepository.findFirstByWebnovelEpisode_IdAndDeletedAtIsNullOrderByLikeCountDescCreatedAtDesc(episodeId).orElse(null);

        if (comment == null) {
            return CommentResponse.Best.fromEntity(null, 0L);
        }

        Long totalCount = webnovelEpisodeCommentRepository.countByWebnovelEpisode_IdAndDeletedAtIsNull(episodeId);

        if (comment.getLikeCount() == 0) {
            comment = null;
        }

        return CommentResponse.Best.fromEntity(comment, totalCount);
    }

    @Override
    public Set<Long> getLikedCommentIds(Long userId, List<Long> commentIds) {
        List<Long> likedCommentIds = webnovelEpisodeCommentLikeRepository.findLikedCommentIdsByUserIdAndCommentIds(userId, commentIds);

        return new HashSet<>(likedCommentIds);
    }

    @Override
    public Boolean hasLiked(Long userId, Long commentId) {
        return webnovelEpisodeCommentLikeRepository.existsByUser_IdAndWebnovelEpisodeComment_Id(userId, commentId);
    }

    @Override
    public void saveLike(User user, Long commentId) {
        WebnovelEpisodeComment comment = getEpisodeComment(commentId);

        WebnovelEpisodeCommentLike like = WebnovelEpisodeCommentLike.builder()
                .user(user)
                .webnovelEpisodeComment(comment)
                .build();

        webnovelEpisodeCommentLikeRepository.save(like);
        comment.updateLikeCount();
    }

    @Override
    public void deleteLike(Long userId, Long commentId) {
        WebnovelEpisodeComment comment = getEpisodeComment(commentId);
        WebnovelEpisodeCommentLike commentLike = webnovelEpisodeCommentLikeRepository.findByUser_IdAndWebnovelEpisodeComment_Id(userId, commentId).orElseThrow(
                () -> new CustomException(ErrorCode.COMMENT_LIKE_NOT_FOUND)
        );

        webnovelEpisodeCommentLikeRepository.delete(commentLike);

        comment.deleteLikeCount();
    }

    private WebnovelEpisodeComment getEpisodeComment(Long commentId) {
        WebnovelEpisodeComment comment = webnovelEpisodeCommentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ErrorCode.COMMENT_NOT_FOUND)
        );

        if (comment.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.COMMENT_ALREADY_DELETED);
        }

        return comment;
    }

    @Override
    public EpisodePurchaseService.EpisodeInfo getEpisodeInfo(Long episodeId, PurchaseType purchaseType) {
        WebnovelEpisode episode = getWebnovelEpisode(episodeId);

        if (episode.getParentContent().getDeletedAt() != null) {
            throw new CustomException(ErrorCode.CONTENT_IS_DELETED);
        }

        Integer price = (purchaseType == PurchaseType.OWN) ? episode.getPurchasePrice() : episode.getRentalPrice();

        return new EpisodePurchaseService.EpisodeInfo(
                episode.getParentContent(),
                price,
                episode
        );
    }

    @Override
    public Optional<Integer> getMaxEpisodeNum(Long contentId) {
        return webnovelEpisodeRepository.findMaxEpisodeNumByContentId(contentId);
    }

    @Override
    public List<Object[]> getGroupByStats(Long contentId) {
        return webnovelEpisodeRepository.countGroupByStats(contentId);
    }

    @Override
    public Page<EpisodeList> getAllEpisodesByContent(Long contentId, Pageable pageable) {
        return webnovelEpisodeRepository.findAllByWebnovel_id(contentId, pageable);
    }

    @Override
    public Page<EpisodeList> getEpisodesByEpisodeStatus(Long contentId, EpisodeStatus episodeStatus, Pageable pageable) {
        return webnovelEpisodeRepository.findByWebnovel_IdAndEpisodeStatus(contentId, episodeStatus, pageable);
    }

    @Override
    public void deleteAllEpisode(Long contentId) {
        LocalDateTime deletedAt = LocalDateTime.now();

        webnovelEpisodeRepository.bulkUpdateDeletedAt(contentId, deletedAt);
    }
}
