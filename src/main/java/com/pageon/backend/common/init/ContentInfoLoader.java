package com.pageon.backend.common.init;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
@Getter
@Component
public class ContentInfoLoader {

    // content_id → [episode_min, episode_max]
    private final Map<Long, long[]> contentEpisodeMap = new HashMap<>();
    private final Map<Long, Long> contentCreatorMap = new HashMap<>();
    private final List<Long> webtoonContentIds = new ArrayList<>();
    private final List<Long> webnovelContentIds = new ArrayList<>();

    @PostConstruct
    public void load() throws Exception {
        log.info("=== content_info.csv 로딩 시작 ===");
        ClassPathResource resource = new ClassPathResource("data/content_info.csv");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                String[] parts = line.split(",");
                long contentId = Long.parseLong(parts[0].trim());
                long creatorId = Long.parseLong(parts[1].trim());
                String dtype = parts[2].trim();
                long episodeMin = Long.parseLong(parts[3].trim());
                long episodeMax = Long.parseLong(parts[4].trim());

                contentEpisodeMap.put(contentId, new long[]{episodeMin, episodeMax});
                contentCreatorMap.put(contentId, creatorId);
                if ("WEBTOON".equals(dtype)) {
                    webtoonContentIds.add(contentId);
                } else {
                    webnovelContentIds.add(contentId);
                }
            }
        }
        log.info("=== content_info.csv 로딩 완료 - 총 {}개 ===", contentEpisodeMap.size());
    }

    public long randomContentId(boolean isWebtoon, java.util.Random random) {
        List<Long> ids = isWebtoon ? webtoonContentIds : webnovelContentIds;
        return ids.get(random.nextInt(ids.size()));
    }

    public long getCreatorId(long contentId) {
        return contentCreatorMap.get(contentId);
    }

    public long randomEpisodeId(long contentId, java.util.Random random) {
        long[] range = contentEpisodeMap.get(contentId);
        if (range == null) return 1L;
        return range[0] + (long) (random.nextDouble() * (range[1] - range[0] + 1));
    }
}

