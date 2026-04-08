package com.pageon.backend.service;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.NotificationType;
import com.pageon.backend.dto.record.EpisodeNotificationEvent;
import com.pageon.backend.dto.record.SettlementNotificationEvent;
import com.pageon.backend.dto.response.SseData;
import com.pageon.backend.entity.Notification;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.NotificationRepository;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.security.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private static final Long DEFAULT_TIMEOUT = 30 * 60 * 1000L;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final JwtProvider jwtProvider;

    public SseEmitter subscribe(String token) {
        Claims claims = jwtProvider.validateAndGetClaims(token);
        Long userId = claims.get("userId", Long.class);
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);

        sendToClient(userId, "connect","connected", "connected");

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(userId);
        });
        emitter.onError((e) -> {
            emitter.complete();
            emitters.remove(userId);
        });

        return emitter;
    }

    public void updateEpisode(EpisodeNotificationEvent event) {
        User user = userRepository.findByIdAndDeletedAtIsNull(event.userId()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );
        if (event.notificationType() == NotificationType.NEW_EPISODE_RELEASED) {
            SseData sseData = buildEpisodeUpdateNotification(event);
            sendToClient(user.getId(),"pageon", sseData, "Episode Published");
        } else if (event.notificationType() == NotificationType.SCHEDULED_EPISODE_POSTED) {
            SseData sseData = buildScheduledEpisodeUploadNotification(event);
            sendToClient(user.getId(), "pageon", sseData, "Episode Published");
        }

    }

    public void payoutSettlement(SettlementNotificationEvent event) {
        User user = userRepository.findByIdAndDeletedAtIsNull(event.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String message = (event.notificationType() == NotificationType.SETTLEMENT_SUCCESS)
                ? String.format("%d월 정산금이 지급되었습니다.", event.month())
                : String.format("%d월 정산금 지급이 실패하였습니다.", event.month());

        String sseType = (event.notificationType() == NotificationType.SETTLEMENT_SUCCESS)
                ? "Settlement SUCCESS"
                : "Settlement FAILED";

        SseData sseData = new SseData(
                message,
                "/creators/revenue/history?settlementId=" + event.settlementId()
        );

        sendToClient(user.getId(), "pageon", sseData, sseType);
    }

    private SseData buildScheduledEpisodeUploadNotification(EpisodeNotificationEvent event) {
        String content = String.format("'%s'의 예약된 에피소드가 업로드 됐습니다.", event.contentTitle());

        String redirectUrl = "/creators/contents/episodes/dashboard?contentId=" + event.contentId();

        Notification notification = Notification.builder()
                .userId(event.userId())
                .feedId(event.contentId())
                .content(content)
                .redirectUrl(redirectUrl)
                .notificationType(event.notificationType())
                .build();

        notificationRepository.save(notification);

        return new SseData(content, redirectUrl);
    }

    private SseData buildEpisodeUpdateNotification(EpisodeNotificationEvent event) {
        String content = String.format("'%s'의 새로운 에피소드가 업데이트 됐습니다.", event.contentTitle());

        String redirectUrl = "";
        if (event.contentType() == ContentType.WEBTOON) {
            redirectUrl = "/webtoons/" + event.contentId();
        } else {
            redirectUrl = "/webnovels/" + event.contentId();
        }

        Notification notification = Notification.builder()
                .userId(event.userId())
                .feedId(event.contentId())
                .content(content)
                .redirectUrl(redirectUrl)
                .notificationType(event.notificationType())
                .build();

        notificationRepository.save(notification);

        return new SseData(content, redirectUrl);
    }


    private <T> void sendToClient(Long userId, String name, T data, String comment) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(userId))
                        .name(name)
                        .data(data)
                        .comment(comment)
                );
            } catch (Exception e) {
                emitters.remove(userId);
                emitter.completeWithError(e);
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (Exception e) {
                emitter.complete();
                emitters.remove(id);
            }
        });
    }

}
