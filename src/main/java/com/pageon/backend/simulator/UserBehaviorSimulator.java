package com.pageon.backend.simulator;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.dto.request.EpisodeCommentRequest;
import com.pageon.backend.dto.request.EpisodeRatingRequest;
import com.pageon.backend.dto.request.ContentInfoRequest;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.WebnovelEpisode;
import com.pageon.backend.entity.WebtoonEpisode;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.ContentRepository;
import com.pageon.backend.repository.WebnovelEpisodeRepository;
import com.pageon.backend.repository.WebtoonEpisodeRepository;
import com.pageon.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBehaviorSimulator {


    private final ContentRepository contentRepository;
    private final EpisodeService episodeService;
    private final EpisodeCommentService episodeCommentService;
    private final WebnovelEpisodeRepository webnovelEpisodeRepository;
    private final WebtoonEpisodeRepository webtoonEpisodeRepository;
    private final EpisodePurchaseService episodePurchaseService;
    private final Random random = new Random();
    private final ContentService contentService;


    //    @Scheduled(fixedDelay = 1000)
    public void simulateRandomActivity() {

        Long userId = random.nextLong(10000) + 1;

        ContentInfoRequest contentInfo = getRandomContent();

        /*
            Action 랜덤 결정
            0: 에피소드 조회, 1: 댓글 작성, 2: 에피소드 별점,
            3: 콘텐츠 관심 등록, 4: 에피소드 대여, 5: 에피소드 구매
        */
        int action = random.nextInt(4);

        switch (action) {
            case 0:
                log.info("Simulating random activity: VIEW");
                actionContentsView(userId, contentInfo);
                break;
            case 1:
                log.info("Simulating random activity: COMMENT");
                actionEpisodeComments(userId, contentInfo);
                break;
            case 2:
                log.info("Simulating random activity: RATING");
                actionEpisodeRating(userId, contentInfo);
                break;
            case 3:
                log.info("Simulating random activity: INTEREST");
                actionContentsInterest(userId, contentInfo);
                break;
            case 4:
                log.info("Simulating random activity: EPISODE RENTAL");
                actionContentsRental(userId, contentInfo);
                break;
            case 5:
                log.info("Simulating random activity: EPISODE PURCHASE");
                actionContentsPurchase(userId, contentInfo);
                break;
        }



    }

    private ContentInfoRequest getRandomContent() {

        Long contentId = random.nextLong(700) + 1;

        Content content = contentRepository.findById(contentId).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        Long episodeId = 0L;

        if (content.getDtype().equals("WEBNOVEL")) {
            List<WebnovelEpisode> episodes = webnovelEpisodeRepository.findByWebnovelId(contentId);

            int randomIndex = random.nextInt(episodes.size());
            episodeId = episodes.get(randomIndex).getId();
        } else if (content.getDtype().equals("WEBTOON")) {
            List<WebtoonEpisode> episodes = webtoonEpisodeRepository.findByWebtoonId(contentId);

            int randomIndex = random.nextInt(episodes.size());
            episodeId = episodes.get(randomIndex).getId();
        }

        return ContentInfoRequest.builder()
                .contentId(contentId)
                .contentType(ContentType.valueOf(content.getDtype()))
                .episodeId(episodeId)
                .build();
    }

    private void actionContentsView(Long userId, ContentInfoRequest contentInfo) {
        // 구매 내역 확인
        boolean purchaseCheck = episodePurchaseService.checkPurchaseHistory(userId, contentInfo.getContentId(), contentInfo.getEpisodeId());

        boolean rentalRandom = random.nextBoolean();


        if (!purchaseCheck) {
            // 구매 내역 없으면 구매 또는 대여
            if (contentInfo.getContentType() == ContentType.WEBTOON) {
                if (rentalRandom) {
                    actionContentsRental(userId, contentInfo);
                    return;
                }
            }

            actionContentsPurchase(userId, contentInfo);
        } else {
            // 에피소드 페이지로 이동
            if (contentInfo.getContentType() == ContentType.WEBNOVEL) {
                episodeService.getEpisodeDetail(userId, "webnovels", contentInfo.getEpisodeId());
            } else {
                episodeService.getEpisodeDetail(userId, "webtoons", contentInfo.getEpisodeId());
            }
        }

    }

    private String getRandomComment() {
        String[] messages = {
                "오늘 내용도 정말 흥미진진하네요. 잘 읽었습니다.",
                "다음 이야기가 어떻게 전개될지 벌써부터 기대됩니다.",
                "등장인물들의 감정선이 섬세해서 몰입하며 읽었습니다.",
                "시간 가는 줄 모르고 읽었네요. 작가님 고생 많으셨습니다.",
                "그림체가 이야기 분위기와 참 잘 어울리는 것 같아요.",
                "날씨가 쌀쌀한데 작가님 감기 조심하시고 힘내세요.",
                "앞부분 내용을 다시 읽어보니 감회가 새롭네요. 정주행 중입니다.",
                "마지막까지 좋은 결말로 이어지기를 응원하겠습니다.",
                "일주일을 기다리는 즐거움이 생겼습니다. 감사합니다.",
                "좋은 작품 연재해 주셔서 감사합니다. 다음 주도 기다릴게요."
        };
        return messages[random.nextInt(messages.length)];
    }

    private void actionEpisodeComments(Long userId, ContentInfoRequest contentInfo) {
        // 구매 내역 확인
        // 구매 내역 없으면 구매 또는 대여
        // 에피소드 페이지로 이동
        actionContentsView(userId, contentInfo);

        // 댓글 작성
        boolean isSpoiler = random.nextBoolean();

        EpisodeCommentRequest commentRequest = new EpisodeCommentRequest(getRandomComment(), isSpoiler);

        if (contentInfo.getContentType() == ContentType.WEBTOON) {
            episodeCommentService.createComment(userId, "webnovels", contentInfo.getEpisodeId(), commentRequest);
        } else {
            episodeCommentService.createComment(userId, "webtoons", contentInfo.getEpisodeId(), commentRequest);
        }
    }

    private void actionEpisodeRating(Long userId, ContentInfoRequest contentInfo) {
        // 구매 내역 확인
        // 구매 내역 없으면 구매 또는 대여
        // 에피소드 페이지로 이동
        actionContentsView(userId, contentInfo);

        // 별점 입력
        int randomScore = random.nextInt(5) + 6;
        EpisodeRatingRequest ratingRequest = new EpisodeRatingRequest(randomScore);
        if (contentInfo.getContentType() == ContentType.WEBTOON) {
            episodeService.rateEpisode(userId, "webtoons", contentInfo.getEpisodeId(),  ratingRequest);
        } else {
            episodeService.rateEpisode(userId, "webnovels", contentInfo.getEpisodeId(), ratingRequest);
        }
    }

    private void actionContentsInterest(Long userId, ContentInfoRequest contentInfo) {
        // 콘텐츠 관심 등록
        contentService.toggleInterest(userId, contentInfo.getContentId());
    }

    private void actionContentsRental(Long userId, ContentInfoRequest contentInfo) {
        // 구매 내역 확인
        boolean purchaseCheck = episodePurchaseService.checkPurchaseHistory(userId, contentInfo.getContentId(), contentInfo.getEpisodeId());
        // 구매 내역 없으면 에피소드 대여
        String contentType = contentInfo.getContentType().toString().toLowerCase() + "s";
        if (!purchaseCheck) {
            episodePurchaseService.createPurchaseHistory(userId, contentType, contentInfo.getEpisodeId(), PurchaseType.RENT);
        }
        // 에피소드 페이지로 이동
        episodeService.getEpisodeDetail(userId, contentType, contentInfo.getEpisodeId());
    }

    private void actionContentsPurchase(Long userId, ContentInfoRequest contentInfo) {
        // 구매 내역 확인
        boolean purchaseCheck = episodePurchaseService.checkPurchaseHistory(userId, contentInfo.getContentId(), contentInfo.getEpisodeId());
        boolean actionCheck = random.nextBoolean();

        String contentType = contentInfo.getContentType().toString().toLowerCase() + "s";

        int randomScore = random.nextInt(5) + 6;
        EpisodeRatingRequest ratingRequest = new EpisodeRatingRequest(randomScore);

        boolean isSpoiler = random.nextBoolean();
        EpisodeCommentRequest commentRequest = new EpisodeCommentRequest(getRandomComment(), isSpoiler);
        // 구매 내역 없으면 에피소드 구매
        if (!purchaseCheck) {
            episodePurchaseService.createPurchaseHistory(userId, contentType, contentInfo.getEpisodeId(), PurchaseType.OWN);
        }

        // 에피소드 페이지로 이동
        if (contentInfo.getContentType() == ContentType.WEBTOON) {
            episodeService.getEpisodeDetail(userId, contentType, contentInfo.getEpisodeId());
            if (actionCheck) {
                episodeService.rateEpisode(userId, contentType, contentInfo.getEpisodeId(), ratingRequest);
                episodeCommentService.createComment(userId, contentType, contentInfo.getEpisodeId(), commentRequest);
            }
        } else {
            episodeService.getEpisodeDetail(userId, contentType, contentInfo.getEpisodeId());
            if (actionCheck) {
                episodeService.rateEpisode(userId, contentType, contentInfo.getEpisodeId(), ratingRequest);
                episodeCommentService.createComment(userId, contentType, contentInfo.getEpisodeId(), commentRequest);
            }

        }
    }




}
