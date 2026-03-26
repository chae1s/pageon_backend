package com.pageon.backend.config;

import com.opencsv.CSVReader;
import com.pageon.backend.common.enums.*;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import com.pageon.backend.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@Order(3)
@Profile("!test")
@Transactional
@RequiredArgsConstructor
public class InitContentData implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final KeywordRepository keywordRepository;
    private final CreatorRepository creatorRepository;
    private final WebnovelRepository webnovelRepository;
    private final FileUploadService fileUploadService;
    private final WebtoonRepository webtoonRepository;
    private final WebnovelEpisodeRepository webnovelEpisodeRepository;
    private final WebtoonEpisodeRepository webtoonEpisodeRepository;
    private final ContentKeywordRepository contentKeywordRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initCreators();
        initCategory();
        initKeywords();
        initWebnovels();
        initWebtoons();
        initWebtoonEpisode();
        initWebnovelEpisode();
    }

    private void initCreators() {
        if (creatorRepository.count() > 0) {
            return;
        }

        try {
            InputStream inputStream = getClass().getResourceAsStream("/data/creators.csv");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            CSVReader csvReader = new CSVReader(inputStreamReader);

            String [] line;
            while ((line = csvReader.readNext()) != null) {
                User user = userRepository.findByIdAndDeletedAtIsNull(Long.valueOf(line[0])).orElseThrow(() -> new RuntimeException("user 없음"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate birthDate = LocalDate.parse(line[5], formatter);
                user.updateIdentityVerification(line[3], line[4], birthDate, Gender.valueOf(line[6]), line[7], true, IdentityProvider.DANAL);
                userRepository.save(user);

                Creator creators = new Creator(line[1], user, ContentType.valueOf(line[2]));
                creatorRepository.save(creators);

            }

        } catch (Exception e) {
            log.error("에러 발생: {}", e);
        }
    }

    private void initCategory() {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(new Category("장르"));
            categoryRepository.save(new Category("소재"));
            categoryRepository.save(new Category("배경"));
            categoryRepository.save(new Category("분위기"));
            categoryRepository.save(new Category("형식/기타"));
            categoryRepository.save(new Category("카테고리 미배정"));
        }
    }

    public void initKeywords() {
        if (keywordRepository.count() > 0) {
            return;
        }

        try {
            InputStream inputStream = getClass().getResourceAsStream("/data/keywords.csv");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            CSVReader csvReader = new CSVReader(inputStreamReader);
            List<Keyword> keywords = new ArrayList<>();
            String [] line;
            while ((line = csvReader.readNext()) != null) {
                Category category = categoryRepository.findById(Long.valueOf(line[0])).orElseThrow(() -> new RuntimeException("카테고리 없음"));

                Keyword keyWord = new Keyword(category, line[1].trim());

                keywords.add(keyWord);

            }

            keywordRepository.saveAll(keywords);

        } catch (Exception e) {
            log.error("에러 발생: {}", e.getMessage());
        }
    }

    public void initWebnovels() {
        if (webnovelRepository.count() > 0) {
            return;
        }

        InputStream inputStream = getClass().getResourceAsStream("/data/webnovels.csv");
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        CSVReader csvReader = new CSVReader(inputStreamReader);
        List<Webnovel> webnovels = new ArrayList<>();
        String[] line;
        int i = 1;
        try {
            while ((line = csvReader.readNext()) != null) {
                Creator creator = creatorRepository.findById(Long.parseLong(line[3])).orElseThrow(() -> new CustomException(ErrorCode.CREATER_NOT_FOUND));

                String absolutePath = "C:/Users/user/Desktop/project/pageon_images/webnovels/" + line[4];
                File file = new File(absolutePath);
                log.info("file");
                String s3Url = fileUploadService.localFileUpload(file, String.format("webnovels/%d", i++));
                log.info("s3Url");

                log.info("s3 저장 후");
                Webnovel webnovel = Webnovel.builder()
                        .title(line[0])
                        .description(line[1])
                        .creator(creator)
                        .cover(s3Url)
                        .serialDay(SerialDay.valueOf(line[6]))
                        .status(SeriesStatus.valueOf(line[7]))
                        .viewCount(Long.parseLong(line[5]))
                        .build();

                webnovelRepository.save(webnovel);

                separateKeywords(webnovel, line[2]);
            }

        } catch (Exception e) {
            log.error("에러 발생: {}", e.getMessage());
            throw new RuntimeException();
        }


    }

    public void initWebtoons() {
        if (webtoonRepository.count() > 20) {
            return;
        }
        InputStream inputStream = getClass().getResourceAsStream("/data/webtoons.csv");
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        CSVReader csvReader = new CSVReader(inputStreamReader);

        String[] line;
        int i = 1;
        try {
            while ((line = csvReader.readNext()) != null) {

                Creator creator = creatorRepository.findById(Long.parseLong(line[3])).orElseThrow(() -> new CustomException(ErrorCode.CREATER_NOT_FOUND));
                String absolutePath = "C:/Users/user/Desktop/project/pageon_images/webtoons/" + line[4];
                File file = new File(absolutePath);
                String s3Url = fileUploadService.localFileUpload(file, String.format("webtoons/%d", i++));

                Webtoon webtoon = Webtoon.builder()
                        .title(line[0])
                        .description(line[1])
                        .creator(creator)
                        .cover(s3Url)
                        .serialDay(SerialDay.valueOf(line[5]))
                        .status(SeriesStatus.valueOf(line[6]))
                        .viewCount(Long.parseLong(line[7]))
                        .build();

                webtoonRepository.save(webtoon);

                separateKeywords(webtoon, line[2]);
            }

        } catch (Exception e) {
            log.error("에러 발생: {}", e.getMessage());
            throw new RuntimeException();
        }


    }

    private void separateKeywords(Content content, String line) {
        String[] words = line.replaceAll("\\s", "").split(",");
        LinkedHashMap<String, Keyword> keywordMap = new LinkedHashMap<>();
        log.info("content Id: {}", content.getId());

        Category category = categoryRepository.findById(6L).orElseThrow(() -> new RuntimeException());
        for (String word : words) {
            if (!keywordMap.containsKey(word)) {

                Keyword keyword = keywordRepository.findByName(word).orElseGet(
                        () -> {
                            Keyword newKeyword = new Keyword(category, word);

                            return keywordRepository.save(newKeyword);
                        }
                );

                keywordMap.put(word, keyword);
                ContentKeyword contentKeyword = ContentKeyword.builder()
                        .content(content)
                        .keyword(keyword)
                        .build();

                contentKeywordRepository.save(contentKeyword);
            }

        }


    }

    public void initWebnovelEpisode() {
        if (webnovelEpisodeRepository.count() > 0) {
            return;
        }
        InputStream inputStream = getClass().getResourceAsStream("/data/webnovelEpisode.csv");
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        CSVReader csvReader = new CSVReader(inputStreamReader);
        List<WebnovelEpisode> webnovelEpisodes = new ArrayList<>();
        String[] line;
        int i = 1;
        try {
            while ((line = csvReader.readNext()) != null) {
                Webnovel webnovel = webnovelRepository.findById(Long.parseLong(line[0])).orElseThrow(
                        () -> new CustomException(ErrorCode.WEBNOVEL_NOT_FOUND)
                );

                String filePath = String.format("C:/Users/user/Desktop/project/pageon_txt/%s", line[4]);
                String content = readTextFile(filePath);

                WebnovelEpisode webnovelEpisode = WebnovelEpisode.builder()
                        .episodeNum(Integer.parseInt(line[2]))
                        .episodeTitle(line[3])
                        .webnovel(webnovel)
                        .content(content)
                        .purchasePrice(100)
                        .build();

                webnovelEpisodes.add(webnovelEpisode);

                webnovel.updateEpisode();

            }

            webnovelEpisodeRepository.saveAll(webnovelEpisodes);
        } catch (Exception e) {
            log.error("에러 발생: {}", e.getMessage());
            throw new RuntimeException();
        }
    }

    private String readTextFile(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            StringBuffer sb = new StringBuffer();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initWebtoonEpisode() {
        if (webtoonEpisodeRepository.count() > 0) {
            return;
        }
        InputStream inputStream = getClass().getResourceAsStream("/data/webtoonEpisode.csv");
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        CSVReader csvReader = new CSVReader(inputStreamReader);
        boolean check = true;
        String[] line;
        List<WebtoonEpisode> webtoonEpisodes = new ArrayList<>();
        try {
            while ((line = csvReader.readNext()) != null) {
                Webtoon webtoon = webtoonRepository.findById(Long.parseLong(line[0])).orElseThrow(
                        () -> new CustomException(ErrorCode.WEBTOON_NOT_FOUND)
                );

                List<String> webtoonImages = new ArrayList<>();
                if (check) {
                    webtoonImages = webtoonImageUpload();
                    check = false;
                } else {
                    webtoonImages = webtoonImageUrl();
                }

                WebtoonEpisode webtoonEpisode = WebtoonEpisode.builder()
                        .episodeNum(Integer.parseInt(line[2]))
                        .episodeTitle(line[3])
                        .webtoon(webtoon)
                        .rentalPrice(300)
                        .purchasePrice(500)
                        .build();

                int index = 1;
                for (String imageUrl : webtoonImages) {
                    WebtoonImage webtoonImage = WebtoonImage.builder()
                            .sequence(index++)
                            .imageUrl(imageUrl)
                            .build();

                    webtoonEpisode.addImage(webtoonImage);
                }

                webtoonEpisodes.add(webtoonEpisode);

                webtoon.updateEpisode();

            }
            webtoonEpisodeRepository.saveAll(webtoonEpisodes);
        } catch (Exception e) {
            log.error("에러 발생: {}", e.getMessage());
            throw new RuntimeException();
        }
    }

    // 이미지 업로드 필요할 때 사용
    private List<String> webtoonImageUpload() {
        List<String> images = new ArrayList<>();
        // 에피소드 한 편당 52개의 이미지 업로드
        for (int i = 0; i < 52; i++) {
            File file = new File(String.format("/Users/user/Desktop/project/pageon_images/webtoons/episode/episodeCut%d.png", i + 1));

            String imageUrl = fileUploadService.localFileUpload(file, "webtoons/common/episode/common");

            // s3 url을 list에 저장
            images.add(imageUrl);
        }

        return images;
    }

    private List<String> webtoonImageUrl() {
        List<String> images = new ArrayList<>();

        for (int i = 0; i < 52; i++) {
            String imageUrl = String.format("https://d2ge55k9wic00e.cloudfront.net/webtoons/common/episode/common/episodeCut%d.png",i + 1);

            // s3 url을 list에 저장
            images.add(imageUrl);
        }

        return images;
    }


}
