package com.pageon.backend.service;

import com.pageon.backend.dto.request.EpisodeCommentRequest;
import com.pageon.backend.dto.response.CommentResponse;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.User;
import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.entity.base.EpisodeCommentBase;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.service.provider.EpisodeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DisplayName("EpisodeCommentService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class EpisodeCommentServiceTest {
    @InjectMocks
    private EpisodeCommentService episodeCommentService;
    @Mock
    private List<EpisodeProvider> providers;
    @Mock
    private EpisodeProvider episodeProvider;
    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        lenient().when(providers.stream()).thenReturn(Stream.of(episodeProvider));
        lenient().when(episodeProvider.supports(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("에피소드 댓글 등록 성공")
    void createComment_withValidInput_shouldCommentEpisode() {
        // given
        User user = mock(User.class);
        String text = "댓글 내용";
        boolean isSpoiler = false;
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        EpisodeCommentRequest request = new EpisodeCommentRequest(text, isSpoiler);

        //when
        episodeCommentService.createComment(1L, "webnovels", 100L, request);

        // then
        verify(episodeProvider).saveComment(user, 100L, text, isSpoiler);

    }

    @Test
    @DisplayName("댓글 내용이 비어있으면 CustomException 발생")
    void createComment_withBlankText_shouldThrowCustomException() {
        // given
        String text = "  ";
        boolean isSpoiler = false;
        EpisodeCommentRequest request = new EpisodeCommentRequest(text, isSpoiler);
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> episodeCommentService.createComment(1L, "webnovels", 100L, request)
        );

        // then
        assertEquals(ErrorCode.COMMENT_TEXT_IS_BLANK, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("댓글 내용이 존재하지 않습니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("지원하지 않는 contentType이면 CustomException 발생")
    void getProvider_withInvalidContentType_shouldThrowException() {
        // given
        User user = mock(User.class);
        String text = "댓글 내용";
        boolean isSpoiler = false;
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        EpisodeCommentRequest request = new EpisodeCommentRequest(text, isSpoiler);
        when(episodeProvider.supports(anyString())).thenReturn(false);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> episodeCommentService.createComment(1L, "webnovels", 100L, request));

        assertEquals(ErrorCode.INVALID_CONTENT_TYPE, ErrorCode.valueOf(exception.getErrorCode()));
    }

    @Test
    @DisplayName("에피소드 댓글 등록 성공")
    void updateComment_withValidInput_shouldCommentEpisode() {
        // given

        String newText = "댓글 내용";
        boolean isSpoiler = false;

        EpisodeCommentRequest request = new EpisodeCommentRequest(newText, isSpoiler);

        //when
        episodeCommentService.updateComment(1L, "webnovels", 100L, request);

        // then
        verify(episodeProvider).updateComment(1L, 100L, newText, isSpoiler);

    }

    @Test
    @DisplayName("댓글 내용이 비어있으면 CustomException 발생")
    void updateComment_withBlankText_shouldThrowCustomException() {
        // given
        String newText = "  ";
        boolean isSpoiler = false;
        EpisodeCommentRequest request = new EpisodeCommentRequest(newText, isSpoiler);
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> episodeCommentService.updateComment(1L, "webnovels", 100L, request)
        );

        // then
        assertEquals(ErrorCode.COMMENT_TEXT_IS_BLANK, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("댓글 내용이 존재하지 않습니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("특정 에피소드에 작성된 댓글 목록 조회 성공")
    void getComments_withEpisodeId_shouldReturnComments() {
        // given
        EpisodeCommentBase comment = mockComment();

        Page<EpisodeCommentBase> comments = new PageImpl<>(List.of(comment));
        doReturn(comments).when(episodeProvider).findComments(eq(1L), any(Pageable.class));
        when(episodeProvider.getLikedCommentIds(eq(1L), anyList())).thenReturn(Set.of());
        //when
        Page<CommentResponse.Summary> result = episodeCommentService.getComments(1L, "webnovels", 1L, PageRequest.of(0, 10), "latest");

        // then
        assertNotNull(result);
        verify(episodeProvider).getLikedCommentIds(eq(1L), anyList());

    }

    @Test
    @DisplayName("댓글이 없으면 빈 페이지 반환")
    void getComments_withNoComments_shouldReturnEmptyPage() {
        // given
        doReturn(Page.empty()).when(episodeProvider)
                .findComments(eq(1L), any(Pageable.class));

        // when
        Page<CommentResponse.Summary> result = episodeCommentService.getComments(
                1L, "webnovels", 1L, PageRequest.of(0, 10), "latest");

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("좋아요한 댓글은 isLiked true로 반환")
    void getComments_withLikedComment_shouldReturnIsLikedTrue() {
        // given
        EpisodeCommentBase comment = mockComment();

        Page<EpisodeCommentBase> commentPage = new PageImpl<>(List.of(comment));
        doReturn(commentPage).when(episodeProvider)
                .findComments(eq(1L), any(Pageable.class));

        when(episodeProvider.getLikedCommentIds(eq(1L), anyList()))
                .thenReturn(Set.of(1L));

        // when
        Page<CommentResponse.Summary> result = episodeCommentService.getComments(
                1L, "webnovels", 1L, PageRequest.of(0, 10), "latest");

        // then
        assertTrue(result.getContent().get(0).getIsLiked());
    }

    private EpisodeCommentBase mockComment() {
        Content content = mock(Content.class);

        EpisodeBase episode = mock(EpisodeBase.class);
        when(episode.getParentContent()).thenReturn(content);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getNickname()).thenReturn("닉네임");

        EpisodeCommentBase comment = mock(EpisodeCommentBase.class);
        when(comment.getId()).thenReturn(1L);
        when(comment.getParentEpisode()).thenReturn(episode);
        when(comment.getUser()).thenReturn(user);

        return comment;
    }

    @Test
    @DisplayName("좋아요 안 했으면 좋아요 추가")
    void toggleCommentLike_whenNotLiked_shouldSaveLike() {
        // given
        User user = mock(User.class);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(episodeProvider.hasLiked(1L, 1L)).thenReturn(false);

        // when
        episodeCommentService.toggleCommentLike(1L, "webnovels", 1L);

        // then
        verify(episodeProvider).saveLike(eq(user), eq(1L));
        verify(episodeProvider, never()).deleteLike(any(), any());
    }

    @Test
    @DisplayName("좋아요 했으면 좋아요 취소")
    void toggleCommentLike_whenLiked_shouldDeleteLike() {
        // given
        User user = mock(User.class);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(episodeProvider.hasLiked(1L, 1L)).thenReturn(true);

        // when
        episodeCommentService.toggleCommentLike(1L, "webnovels", 1L);

        // then
        verify(episodeProvider).deleteLike(1L, 1L);
        verify(episodeProvider, never()).saveLike(any(), any());
    }

}