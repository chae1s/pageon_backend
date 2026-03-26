package com.pageon.backend.controller;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.dto.request.*;
import com.pageon.backend.dto.response.*;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReadingHistoryService readingHistoryService;
    private final EpisodeCommentService episodeCommentService;
    private final PointTransactionService pointTransactionService;
    private final ContentService contentService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest signupDto) {
        userService.signup(signupDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam("email") String email) {
        boolean isEmailDuplicate = userService.isEmailDuplicate(email);

        return ResponseEntity.ok(Map.of("isEmailDuplicate", isEmailDuplicate));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam("nickname") String nickname) {
        boolean isNicknameDuplicate = userService.isNicknameDuplicate(nickname);

        return ResponseEntity.ok(Map.of("isNicknameDuplicate", isNicknameDuplicate));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, JwtTokenResponse>> login(@RequestBody LoginRequest loginDto, HttpServletResponse response) {
        log.info("로그인");
        JwtTokenResponse jwtTokenResponse = userService.login(loginDto, response);

        return ResponseEntity.ok(Map.of("success", jwtTokenResponse));
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal PrincipalUser principalUser, HttpServletRequest request, HttpServletResponse response) {

        userService.logout(principalUser, request, response);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-password")
    public ResponseEntity<Map<String, String>> passwordFind(@RequestBody FindPasswordRequest passwordDto) {
        Map<String, String> result = userService.passwordFind(passwordDto);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(@AuthenticationPrincipal PrincipalUser principalUser) {

        return ResponseEntity.ok(userService.getMyInfo(principalUser.getId()));
    }

    @PostMapping("/check-password")
    public ResponseEntity<Map<String, Boolean>> checkPassword(@AuthenticationPrincipal PrincipalUser user, @RequestBody Map<String, String> body) {
        String password = body.get("password");
        boolean isCorrect = userService.checkPassword(user.getId(), password);

        return ResponseEntity.ok(Map.of("isCorrect", isCorrect));
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal PrincipalUser principalUser, @RequestBody UserUpdateRequest request) {
        userService.updateProfile(principalUser.getId(), request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @AuthenticationPrincipal PrincipalUser principalUser, @RequestBody UserDeleteRequest userDeleteRequest, HttpServletRequest request
    ) {

        log.info("탈퇴");
        return ResponseEntity.ok(userService.deleteAccount(principalUser.getId(), userDeleteRequest, request));
    }

    @GetMapping("/check-identity")
    public ResponseEntity<Boolean> checkIdentityVerification(@AuthenticationPrincipal PrincipalUser principalUser){

        return ResponseEntity.ok(userService.checkIdentityVerification(principalUser.getId()));
    }

    @GetMapping("/comments")
    public ResponseEntity<PageResponse<CommentResponse.MyComment>> getMyComments(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam("contentType") String contentType
    ) {
        Page<CommentResponse.MyComment> comments = episodeCommentService.getMyComments(principalUser.getId(), contentType, pageable);

        return ResponseEntity.ok(new PageResponse<>(comments));
    }

    @GetMapping("/interests/{contentType}")
    public ResponseEntity<PageResponse<ContentResponse.InterestContent>> getMyInterests(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            Pageable pageable,
            @RequestParam("sort") String sort
    ) {

        Page<ContentResponse.InterestContent> contents = contentService.getInterestContents(principalUser.getId(), contentType, pageable, sort);

        return ResponseEntity.ok(new PageResponse<>(contents));
    }

    @GetMapping("/reading-histories/{contentType}")
    public ResponseEntity<PageResponse<ContentResponse.RecentRead>> getReadingHistories(
            @AuthenticationPrincipal PrincipalUser principalUser, @PathVariable String contentType, @RequestParam("sort") String sort, Pageable pageable
    ) {

        Page<ContentResponse.RecentRead> readingContents = contentService.getReadingHistory(principalUser.getId(), contentType, sort, pageable);

        return ResponseEntity.ok(new PageResponse<>(readingContents));
    }

    @GetMapping("/reading-histories/today")
    public ResponseEntity<List<ContentResponse.Simple>> getReadingHistoriesToday(@AuthenticationPrincipal PrincipalUser principalUser) {

        List<ContentResponse.Simple> contentSimpleResponses = contentService.getTodayReadingHistory(principalUser.getId());

        return ResponseEntity.ok(contentSimpleResponses);
    }

    @GetMapping("/points/history")
    public ResponseEntity<PageResponse<PointTransactionResponse>> getPointHistory (
            @AuthenticationPrincipal PrincipalUser principalUser,
            @RequestParam("type") String transactionType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PointTransactionResponse> pointTransactionResponses = pointTransactionService.getPointHistory(principalUser.getId(), transactionType, pageable);

        return ResponseEntity.ok(new PageResponse<>(pointTransactionResponses));
    }

    @GetMapping("/points/me")
    public ResponseEntity<Integer> getMyPoint (@AuthenticationPrincipal PrincipalUser principalUser) {

        Integer pointBalance = userService.getMyPoint(principalUser.getId());

        return ResponseEntity.ok(pointBalance);
    }
}
