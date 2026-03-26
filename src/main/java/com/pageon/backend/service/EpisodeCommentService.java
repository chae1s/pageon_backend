package com.pageon.backend.service;

import com.pageon.backend.common.utils.PageableUtil;
import com.pageon.backend.dto.request.EpisodeCommentRequest;
import com.pageon.backend.dto.response.CommentResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.entity.base.EpisodeCommentBase;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.service.provider.EpisodeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeCommentService {
    private final List<EpisodeProvider> providers;
    private final UserRepository userRepository;

    @Transactional
    public void createComment(Long userId, String contentType, Long episodeId, EpisodeCommentRequest request) {
        final String text = request.getText();
        final Boolean isSpoiler = request.getIsSpoiler();

        if (text.isBlank()) {
            throw new CustomException(ErrorCode.COMMENT_TEXT_IS_BLANK);
        }

        User user = userRepository.getReferenceById(userId);
        EpisodeProvider provider = getProvider(contentType);
        provider.saveComment(user, episodeId, text, isSpoiler);

    }

    @Transactional
    public void updateComment(Long userId, String contentType, Long episodeId, EpisodeCommentRequest request) {
        final String newText = request.getText();
        final Boolean isSpoiler = request.getIsSpoiler();

        if (newText.isBlank()) {
            throw new CustomException(ErrorCode.COMMENT_TEXT_IS_BLANK);
        }
        EpisodeProvider provider = getProvider(contentType);

        provider.updateComment(userId, episodeId, newText, isSpoiler);
    }

    @Transactional
    public void deleteComment(Long userId, String contentType, Long commentId) {

        EpisodeProvider provider = getProvider(contentType);
        provider.deleteComment(userId, commentId);

    }

    @Transactional(readOnly = true)
    public Page<CommentResponse.Summary> getComments(Long userId, String contentType, Long episodeId, Pageable pageable, String sort) {
        Pageable commentPageable = PageableUtil.commentPageable(pageable, sort);

        EpisodeProvider provider = getProvider(contentType);
        Page<? extends EpisodeCommentBase> comments = provider.findComments(episodeId, commentPageable);

        Set<Long> likedCommentIds = Collections.emptySet();

        if (userId != null) {
            List<Long> commentIds = comments.getContent().stream()
                    .map(EpisodeCommentBase::getId)
                    .toList();

            likedCommentIds = provider.getLikedCommentIds(userId, commentIds);
        }

        final Set<Long> finalLikedIds = likedCommentIds;

        return comments.map(c -> {
            Boolean isLiked = finalLikedIds.contains(c.getId());

            return CommentResponse.Summary.fromEntity(c, userId, c.getParentEpisode(), isLiked);
        });
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse.MyComment> getMyComments(Long userId, String contentType, Pageable pageable) {
        EpisodeProvider provider = getProvider(contentType);

        Page<? extends  EpisodeCommentBase> comments = provider.findMyComments(userId, pageable);

        return comments.map(c -> CommentResponse.MyComment.fromEntity(c, c.getParentEpisode()));
    }

    @Transactional
    public void toggleCommentLike(Long userId, String contentType, Long commentId) {
        User user = userRepository.getReferenceById(userId);

        EpisodeProvider provider = getProvider(contentType);

        Boolean hasLiked = provider.hasLiked(userId, commentId);

        if (hasLiked) {
            provider.deleteLike(userId, commentId);
        } else {
            provider.saveLike(user, commentId);
        }
    }

    private EpisodeProvider getProvider(String contentType) {
        return providers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }
}
