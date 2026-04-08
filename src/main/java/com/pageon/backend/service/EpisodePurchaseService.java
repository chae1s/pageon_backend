package com.pageon.backend.service;

import com.pageon.backend.dto.record.ActionLogEvent;
import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import com.pageon.backend.service.handler.EpisodeActionHandler;
import com.pageon.backend.service.provider.EpisodeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodePurchaseService {

    private final List<EpisodeProvider> providers;
    private final UserRepository userRepository;
    private final EpisodePurchaseRepository episodePurchaseRepository;
    private final PointTransactionService pointTransactionService;
    private final IdempotentService idempotentService;
    private final EpisodeActionHandler episodeActionHandler;


    public record EpisodeInfo(Content content, Integer episodePrice, EpisodeBase episodeBase) {}

    @Transactional
    public void createPurchaseHistory(Long userId, String contentType, Long episodeId, PurchaseType purchaseType) {

        String[] key = {String.valueOf(userId), contentType, purchaseType.toString(), String.valueOf(episodeId)};
        idempotentService.isValidIdempotent(Arrays.asList(key));

        log.info("[START] createPurchaseHistory: userId = {}, contentType = {}, episodeId = {}, purchaseType = {}",
                userId, contentType, episodeId, purchaseType
        );

        User user = userRepository.findByIdWithLock(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        EpisodeProvider provider = getProvider(contentType);
        EpisodeInfo episodeInfo = provider.getEpisodeInfo(episodeId, purchaseType);

        Integer episodePrice = episodeInfo.episodePrice;

        Content content = episodeInfo.content;


        if (user.getPointBalance() < episodePrice) {
            throw new CustomException(ErrorCode.INSUFFICIENT_POINTS);
        }

        // 구매, 대여, 재대여 구분
        EpisodePurchase episodePurchase = validateEpisodePurchase(user, ContentType.fromUrlPath(contentType), content, episodeId, purchaseType);

        String description = String.format("%s %d화 %s",
                content.getTitle(),
                episodeInfo.episodeBase.getEpisodeNum(),
                purchaseType == PurchaseType.OWN ? "소장" : "대여"
        );


        pointTransactionService.usePoint(user, episodePrice, description, episodePurchase.getId(), content);

    }

    public Boolean checkPurchaseHistory(Long userId, Long contentId, Long episodeId) {
        EpisodePurchase episodePurchase = episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(userId, contentId, episodeId).orElse(null);

        if (episodePurchase == null) {
            return false;
        }

        log.info("에피소드 구매 확인: {}", episodePurchase.getPurchaseType());
        if (episodePurchase.getPurchaseType() == PurchaseType.OWN) {
            return true;
        } else {
            LocalDateTime now = LocalDateTime.now();

            return episodePurchase.getExpiredAt().isAfter(now);
        }
    }


    private EpisodePurchase validateEpisodePurchase(User user, ContentType contentType, Content content, Long episodeId, PurchaseType purchaseType) {
        log.info("Validate episode purchase or rent: userId = {}, contentType = {}, episodeId = {}", user.getId(), contentType, episodeId);

        EpisodePurchase episodePurchase
                = episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(user.getId(), content.getId(), episodeId).orElse(null);

        if (episodePurchase == null) {
            if (purchaseType == PurchaseType.OWN) {
                return purchaseEpisode(user, content, contentType, episodeId);
            } else {
                return rentEpisode(user, content, contentType, episodeId);
            }
        } else {
            if (episodePurchase.getPurchaseType() == PurchaseType.OWN) {
                throw new CustomException(ErrorCode.EPISODE_ALREADY_PURCHASE);
            } else {
                LocalDateTime now = LocalDateTime.now();

                if (episodePurchase.getExpiredAt().isAfter(now)) {
                    throw new CustomException(ErrorCode.EPISODE_ALREADY_RENTAL);
                } else {
                    if (purchaseType == PurchaseType.OWN) {
                        episodePurchase.upgradeToPurchase();
                    } else {
                        episodePurchase.extendRental(LocalDateTime.now().plusDays(3));
                        episodeActionHandler.handlePurchaseEpisode(user.getId(), content.getId(), contentType, ActionType.RENTAL);
                    }
                }
            }
        }

        return episodePurchase;


    }

    private EpisodePurchase purchaseEpisode(User user, Content content, ContentType contentType, Long episodeId) {
        EpisodePurchase episodePurchase = EpisodePurchase.builder()
                .user(user)
                .content(content)
                .episodeId(episodeId)
                .purchaseType(PurchaseType.OWN)
                .build();

        episodeActionHandler.handlePurchaseEpisode(user.getId(), content.getId(), contentType, ActionType.PURCHASE);

        return episodePurchaseRepository.save(episodePurchase);

    }

    private EpisodePurchase rentEpisode(User user, Content content, ContentType contentType, Long episodeId) {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(3);

        EpisodePurchase episodePurchase = EpisodePurchase.builder()
                .user(user)
                .content(content)
                .episodeId(episodeId)
                .purchaseType(PurchaseType.RENT)
                .expiredAt(expiredAt)
                .build();

        episodeActionHandler.handlePurchaseEpisode(user.getId(), content.getId(), contentType, ActionType.RENTAL);

        return episodePurchaseRepository.save(episodePurchase);
    }

    private EpisodeProvider getProvider(String contentType) {
        return providers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }

}
