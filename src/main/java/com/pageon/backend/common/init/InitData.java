package com.pageon.backend.common.init;

import com.opencsv.CSVReader;
import com.pageon.backend.entity.Category;
import com.pageon.backend.entity.Keyword;
import com.pageon.backend.entity.Role;
import com.pageon.backend.repository.*;
import com.pageon.backend.repository.episode.WebnovelEpisodeRepository;
import com.pageon.backend.repository.episode.WebtoonEpisodeRepository;
import com.pageon.backend.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(1)
@Profile("!test")
@RequiredArgsConstructor
public class InitData implements ApplicationRunner {
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final KeywordRepository keywordRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initRoles();
        initCategory();
        initKeywords();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role("ROLE_USER"));
            roleRepository.save(new Role("ROLE_CREATOR"));
            roleRepository.save(new Role("ROLE_ADMIN"));
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

}
