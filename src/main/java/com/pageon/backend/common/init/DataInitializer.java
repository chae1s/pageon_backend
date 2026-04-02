package com.pageon.backend.common.init;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ContentInfoLoader contentInfoLoader;
    private final Random random = new Random();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int USER_MIN = 1;
    private static final int USER_MAX = 50000;

    private static final LocalDateTime START_DATE = LocalDateTime.now().minusMonths(4);
    private static final LocalDateTime END_DATE = LocalDateTime.now();

    @Getter
    private boolean dataInitialized = false;

    @Override
    public void run(String... args) throws Exception {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM episode_purchases", Long.class);
        if (count != null && count > 0) {
            log.info("이미 데이터가 존재합니다. DataInitializer skip");
            dataInitialized = true;
            return;
        }

        log.info("=== DataInitializer 시작 ===");
        insertEpisodePurchasesAndPointTransactions();
        insertChargePointTransactions();
        insertReadingHistories();
        insertInterests();
        insertWebtoonEpisodeComments();
        insertWebnovelEpisodeComments();
        insertWebtoonEpisodeRatings();
        insertWebnovelEpisodeRatings();
        updateAllCounts();
        dataInitialized = true;
        log.info("=== DataInitializer 완료 ===");
    }

    // ── episode_purchases + USE point_transactions + creator_earnings + action_logs ──
    private void insertEpisodePurchasesAndPointTransactions() {
        log.info("episode_purchases + USE point_transactions + creator_earnings + action_logs INSERT 시작");
        int total = 200_000;
        int batchSize = 1000;
        List<Object[]> purchaseBatch = new ArrayList<>(batchSize);

        for (int i = 0; i < total; i++) {
            long userId = randomLong(USER_MIN, USER_MAX);
            boolean isWebtoon = random.nextBoolean();
            long contentId = randomContentId(isWebtoon);
            long episodeId = randomEpisodeId(contentId);
            String dtype = isWebtoon ? "WEBTOON" : "WEBNOVEL";
            String purchaseType = random.nextBoolean() ? "OWN" : "RENT";
            LocalDateTime createdAt = randomDateTime();
            String expiredAt = "RENT".equals(purchaseType) ? format(createdAt.plusDays(30)) : null;

            purchaseBatch.add(new Object[]{userId, contentId, episodeId, purchaseType, expiredAt, dtype, format(createdAt)});

            if (purchaseBatch.size() == batchSize) {
                flushPurchasesAndTransactions(purchaseBatch);
                purchaseBatch.clear();
            }
        }
        if (!purchaseBatch.isEmpty()) flushPurchasesAndTransactions(purchaseBatch);
        log.info("episode_purchases + USE point_transactions + creator_earnings + action_logs INSERT 완료");
    }

    private void flushPurchasesAndTransactions(List<Object[]> purchases) {
        int size = purchases.size();

        // 1. episode_purchases INSERT
        jdbcTemplate.batchUpdate(
                "INSERT INTO episode_purchases " +
                        "(user_id, content_id, episode_id, purchase_type, expired_at, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                purchases.stream().map(r -> new Object[]{r[0], r[1], r[2], r[3], r[4], r[6]}).toList()
        );

        List<Object[]> transactions = new ArrayList<>(size);
        List<Object[]> earnings = new ArrayList<>(size);
        List<Object[]> actionLogs = new ArrayList<>(size);

        for (Object[] purchase : purchases) {
            long userId = ((Number) purchase[0]).longValue();
            long contentId = ((Number) purchase[1]).longValue();
            long creatorId = contentInfoLoader.getCreatorId(contentId);
            String dtype = purchase[5].toString();
            String purchaseType = purchase[3].toString();
            String actionType = "RENT".equals(purchaseType) ? "RENTAL" : "PURCHASE";
            int point = (random.nextInt(5) + 1) * 100;
            int balance = random.nextInt(50000);
            String createdAt = purchase[6].toString();
            String orderId = "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            // 2. point_transactions
            transactions.add(new Object[]{
                    userId, "USE", "COMPLETED", point, point, balance,
                    "에피소드 구매", orderId, createdAt  // domainId 제외
            });

            // 3. creator_earnings
            earnings.add(new Object[]{
                    creatorId, contentId, point, "EARNED", createdAt
            });

            // 4. content_action_logs
            actionLogs.add(new Object[]{contentId, userId, actionType, dtype, 0, createdAt});
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO point_transactions " +
                        "(user_id, transaction_type, transaction_status, amount, point, balance, " +
                        "description, order_id, created_at) " +  // domain_id 제외
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                transactions
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO creator_earnings " +
                        "(creator_id, content_id, point, earning_status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?)",
                earnings
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO content_action_logs " +
                        "(content_id, user_id, action_type, content_type, rating_score, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                actionLogs
        );
    }

    // ── CHARGE point_transactions (100,000건) ─────────────────────────
    private void insertChargePointTransactions() {
        log.info("CHARGE point_transactions INSERT 시작");
        int total = 100_000;
        int batchSize = 1000;
        List<Object[]> batch = new ArrayList<>(batchSize);

        for (int i = 0; i < total; i++) {
            long userId = randomLong(USER_MIN, USER_MAX);
            int amount = (random.nextInt(10) + 1) * 1000;
            int balance = random.nextInt(100000);
            String orderId = "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String paymentKey = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            LocalDateTime paidAt = randomDateTime();

            batch.add(new Object[]{
                    userId, "CHARGE", "COMPLETED", amount, amount, balance,
                    "포인트 충전", orderId, paymentKey, "카드", format(paidAt), format(paidAt)
            });

            if (batch.size() == batchSize) {
                flushChargeTransactions(batch);
                batch.clear();
            }
        }
        flushChargeTransactions(batch);
        log.info("CHARGE point_transactions INSERT 완료");
    }

    private void flushChargeTransactions(List<Object[]> batch) {
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT IGNORE INTO point_transactions " +
                            "(user_id, transaction_type, transaction_status, amount, point, balance, " +
                            "description, order_id, payment_key, payment_method, paid_at, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    batch
            );
        }
    }

    // ── reading_histories + VIEW action_logs (500,000건) ──────────────
    private void insertReadingHistories() {
        log.info("reading_histories + VIEW action_logs INSERT 시작");
        int total = 500_000;
        int batchSize = 1000;
        List<Object[]> batch = new ArrayList<>(batchSize);

        for (int i = 0; i < total; i++) {
            long userId = randomLong(USER_MIN, USER_MAX);
            boolean isWebtoon = random.nextBoolean();
            long contentId = randomContentId(isWebtoon);
            long episodeId = randomEpisodeId(contentId);
            String dtype = isWebtoon ? "WEBTOON" : "WEBNOVEL";
            LocalDateTime lastReadAt = randomDateTime();

            batch.add(new Object[]{userId, contentId, episodeId, dtype, format(lastReadAt), format(lastReadAt)});

            if (batch.size() == batchSize) {
                flushReadingHistories(batch);
                batch.clear();
            }
        }
        flushReadingHistories(batch);
        log.info("reading_histories + VIEW action_logs INSERT 완료");
    }

    private void flushReadingHistories(List<Object[]> batch) {
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT IGNORE INTO reading_histories " +
                            "(user_id, content_id, episode_id, last_read_at, created_at) " +
                            "VALUES (?, ?, ?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[0], r[1], r[2], r[4], r[5]}).toList()
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO content_action_logs " +
                            "(content_id, user_id, action_type, content_type, rating_score, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[1], r[0], "VIEW", r[3], 0, r[5]}).toList()
            );
        }
    }

    // ── interests + INTEREST action_logs (150,000건) ──────────────────
    private void insertInterests() {
        log.info("interests + INTEREST action_logs INSERT 시작");
        int total = 150_000;
        int batchSize = 1000;
        List<Object[]> batch = new ArrayList<>(batchSize);

        for (int i = 0; i < total; i++) {
            long userId = randomLong(USER_MIN, USER_MAX);
            boolean isWebtoon = random.nextBoolean();
            long contentId = randomContentId(isWebtoon);
            String dtype = isWebtoon ? "WEBTOON" : "WEBNOVEL";
            LocalDateTime createdAt = randomDateTime();

            batch.add(new Object[]{userId, contentId, dtype, format(createdAt)});

            if (batch.size() == batchSize) {
                flushInterests(batch);
                batch.clear();
            }
        }
        flushInterests(batch);
        log.info("interests + INTEREST action_logs INSERT 완료");
    }

    private void flushInterests(List<Object[]> batch) {
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT IGNORE INTO interests (user_id, content_id, created_at) VALUES (?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[0], r[1], r[3]}).toList()
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO content_action_logs " +
                            "(content_id, user_id, action_type, content_type, rating_score, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[1], r[0], "INTEREST", r[2], 0, r[3]}).toList()
            );
        }
    }

    // ── webtoon_episode_comments + COMMENT action_logs (50,000건) ─────
    private void insertWebtoonEpisodeComments() {
        log.info("webtoon_episode_comments + COMMENT action_logs INSERT 시작");
        int total = 50_000;
        int batchSize = 1000;
        List<Object[]> batch = new ArrayList<>(batchSize);
        String[] texts = {"재밌어요!", "다음화 기대됩니다", "최고의 웹툰", "스토리가 탄탄해요", "작가님 천재!"};

        for (int i = 0; i < total; i++) {
            long userId = randomLong(USER_MIN, USER_MAX);
            long contentId = randomContentId(true);
            long episodeId = randomEpisodeId(contentId);
            String text = texts[random.nextInt(texts.length)];
            boolean isSpoiler = random.nextInt(10) == 0;
            long likeCount = random.nextInt(100);
            LocalDateTime createdAt = randomDateTime();

            batch.add(new Object[]{userId, contentId, episodeId, text, isSpoiler, likeCount, format(createdAt)});

            if (batch.size() == batchSize) {
                flushWebtoonComments(batch);
                batch.clear();
            }
        }
        flushWebtoonComments(batch);
        log.info("webtoon_episode_comments + COMMENT action_logs INSERT 완료");
    }

    private void flushWebtoonComments(List<Object[]> batch) {
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO webtoon_episode_comments " +
                            "(user_id, webtoon_episode_id, text, is_spoiler, like_count, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[0], r[2], r[3], r[4], r[5], r[6]}).toList()
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO content_action_logs " +
                            "(content_id, user_id, action_type, content_type, rating_score, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[1], r[0], "COMMENT", "WEBTOON", 0, r[6]}).toList()
            );
        }
    }

    // ── webnovel_episode_comments + COMMENT action_logs (50,000건) ────
    private void insertWebnovelEpisodeComments() {
        log.info("webnovel_episode_comments + COMMENT action_logs INSERT 시작");
        int total = 50_000;
        int batchSize = 1000;
        List<Object[]> batch = new ArrayList<>(batchSize);
        String[] texts = {"몰입감 최고!", "반전이 대박", "주인공 너무 멋있어요", "다음 편이 기대돼요", "강추합니다"};

        for (int i = 0; i < total; i++) {
            long userId = randomLong(USER_MIN, USER_MAX);
            long contentId = randomContentId(false);
            long episodeId = randomEpisodeId(contentId);
            String text = texts[random.nextInt(texts.length)];
            boolean isSpoiler = random.nextInt(10) == 0;
            long likeCount = random.nextInt(100);
            LocalDateTime createdAt = randomDateTime();

            batch.add(new Object[]{userId, contentId, episodeId, text, isSpoiler, likeCount, format(createdAt)});

            if (batch.size() == batchSize) {
                flushWebnovelComments(batch);
                batch.clear();
            }
        }
        flushWebnovelComments(batch);
        log.info("webnovel_episode_comments + COMMENT action_logs INSERT 완료");
    }

    private void flushWebnovelComments(List<Object[]> batch) {
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO webnovel_episode_comments " +
                            "(user_id, webnovel_episode_id, text, is_spoiler, like_count, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[0], r[2], r[3], r[4], r[5], r[6]}).toList()
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO content_action_logs " +
                            "(content_id, user_id, action_type, content_type, rating_score, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[1], r[0], "COMMENT", "WEBNOVEL", 0, r[6]}).toList()
            );
        }
    }

    // ── webtoon_episode_ratings + RATING action_logs (80,000건) ───────
    private void insertWebtoonEpisodeRatings() {
        log.info("webtoon_episode_ratings + RATING action_logs INSERT 시작");
        int total = 80_000;
        int batchSize = 1000;
        List<Object[]> batch = new ArrayList<>(batchSize);

        for (int i = 0; i < total; i++) {
            long userId = randomLong(USER_MIN, USER_MAX);
            long contentId = randomContentId(true);
            long episodeId = randomEpisodeId(contentId);
            int score = random.nextInt(4) + 7;
            LocalDateTime createdAt = randomDateTime();

            batch.add(new Object[]{userId, contentId, episodeId, score, format(createdAt)});

            if (batch.size() == batchSize) {
                flushWebtoonRatings(batch);
                batch.clear();
            }
        }
        flushWebtoonRatings(batch);
        log.info("webtoon_episode_ratings + RATING action_logs INSERT 완료");
    }

    private void flushWebtoonRatings(List<Object[]> batch) {
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT IGNORE INTO webtoon_episode_ratings " +
                            "(user_id, webtoon_episode_id, score) VALUES (?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[0], r[2], r[3]}).toList()
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO content_action_logs " +
                            "(content_id, user_id, action_type, content_type, rating_score, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[1], r[0], "RATING", "WEBTOON", r[3], r[4]}).toList()
            );
        }
    }

    // ── webnovel_episode_ratings + RATING action_logs (80,000건) ──────
    private void insertWebnovelEpisodeRatings() {
        log.info("webnovel_episode_ratings + RATING action_logs INSERT 시작");
        int total = 80_000;
        int batchSize = 1000;
        List<Object[]> batch = new ArrayList<>(batchSize);

        for (int i = 0; i < total; i++) {
            long userId = randomLong(USER_MIN, USER_MAX);
            long contentId = randomContentId(false);
            long episodeId = randomEpisodeId(contentId);
            int score = random.nextInt(4) + 7;
            LocalDateTime createdAt = randomDateTime();

            batch.add(new Object[]{userId, contentId, episodeId, score, format(createdAt)});

            if (batch.size() == batchSize) {
                flushWebnovelRatings(batch);
                batch.clear();
            }
        }
        flushWebnovelRatings(batch);
        log.info("webnovel_episode_ratings + RATING action_logs INSERT 완료");
    }

    private void flushWebnovelRatings(List<Object[]> batch) {
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT IGNORE INTO webnovel_episode_ratings " +
                            "(user_id, webnovel_episode_id, score) VALUES (?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[0], r[2], r[3]}).toList()
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO content_action_logs " +
                            "(content_id, user_id, action_type, content_type, rating_score, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    batch.stream().map(r -> new Object[]{r[1], r[0], "RATING", "WEBNOVEL", r[3], r[4]}).toList()
            );
        }
    }

    // ── 집계 UPDATE ───────────────────────────────────────────────────
    private void updateAllCounts() {
        log.info("viewCount, interestCount, rating UPDATE 시작");

        log.info("content viewCount UPDATE 중...");
        jdbcTemplate.update("""
                UPDATE contents c
                SET view_count = (
                    SELECT COUNT(*) FROM reading_histories rh
                    WHERE rh.content_id = c.id
                )
                """);

        log.info("content interestCount UPDATE 중...");
        jdbcTemplate.update("""
                UPDATE contents c
                SET interest_count = (
                    SELECT COUNT(*) FROM interests i
                    WHERE i.content_id = c.id
                )
                """);

        log.info("content rating UPDATE 중... (WEBTOON)");
        jdbcTemplate.update("""
                UPDATE contents c
                SET total_rating_count = (
                    SELECT COUNT(*) FROM webtoon_episode_ratings wr
                    JOIN webtoon_episodes we ON wr.webtoon_episode_id = we.id
                    WHERE we.webtoon_id = c.id
                ),
                total_average_rating = (
                    SELECT COALESCE(AVG(wr.score), 0) FROM webtoon_episode_ratings wr
                    JOIN webtoon_episodes we ON wr.webtoon_episode_id = we.id
                    WHERE we.webtoon_id = c.id
                )
                WHERE c.dtype = 'Webtoon'
                """);

        log.info("content rating UPDATE 중... (WEBNOVEL)");
        jdbcTemplate.update("""
                UPDATE contents c
                SET total_rating_count = (
                    SELECT COUNT(*) FROM webnovel_episode_ratings wr
                    JOIN webnovel_episodes we ON wr.webnovel_episode_id = we.id
                    WHERE we.webnovel_id = c.id
                ),
                total_average_rating = (
                    SELECT COALESCE(AVG(wr.score), 0) FROM webnovel_episode_ratings wr
                    JOIN webnovel_episodes we ON wr.webnovel_episode_id = we.id
                    WHERE we.webnovel_id = c.id
                )
                WHERE c.dtype = 'Webnovel'
                """);

        log.info("content rating UPDATE 완료... (WEBNOVEL)");
    }

    // ── 유틸 ──────────────────────────────────────────────────────────
    private long randomContentId(boolean isWebtoon) {
        return contentInfoLoader.randomContentId(isWebtoon, random);
    }

    private long randomEpisodeId(long contentId) {
        return contentInfoLoader.randomEpisodeId(contentId, random);
    }

    private long randomLong(int min, int max) {
        return min + (long) (random.nextDouble() * (max - min + 1));
    }

    private LocalDateTime randomDateTime() {
        long startEpoch = START_DATE.toEpochSecond(ZoneOffset.UTC);
        long endEpoch = END_DATE.toEpochSecond(ZoneOffset.UTC);
        long randomEpoch = startEpoch + (long) (random.nextDouble() * (endEpoch - startEpoch));
        return LocalDateTime.ofEpochSecond(randomEpoch, 0, ZoneOffset.UTC);
    }

    private String format(LocalDateTime dt) {
        return dt == null ? null : dt.format(FORMATTER);
    }
}