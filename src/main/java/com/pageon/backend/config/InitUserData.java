package com.pageon.backend.config;

import com.opencsv.CSVReader;
import com.pageon.backend.common.enums.Gender;
import com.pageon.backend.common.enums.OAuthProvider;
import com.pageon.backend.common.enums.RoleType;
import com.pageon.backend.entity.Role;
import com.pageon.backend.entity.UserRole;
import com.pageon.backend.entity.User;
import com.pageon.backend.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(2)
@Profile("!test")
@RequiredArgsConstructor
public class InitUserData implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int BATCH_SIZE = 1000;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("--- User data 10000건 saveAll( )로 한번에 DB에 저장 ---");

        initUsersWithSaveAll();

    }

    private void initUsersWithIndividualSave() {
        if (userRepository.count() > 0) {
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info("개별 save() 방식으로 10000개의 사용자 데이터 로딩 시작");


        try {
            InputStream inputStream = getClass().getResourceAsStream("/data/users.csv");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            CSVReader csvReader = new CSVReader(inputStreamReader);
            String [] line;
            while ((line = csvReader.readNext()) != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate birthDate = LocalDate.parse(line[3], formatter);

                User user = User.builder()
                        .email(line[0])
                        .password(passwordEncoder.encode(line[1]))
                        .nickname(line[2])
                        .birthDate(birthDate)
                        .pointBalance(Integer.valueOf(line[4]))
                        .oAuthProvider(OAuthProvider.EMAIL)
                        .gender(Gender.valueOf(line[5]))
                        .termsAgreed(true)
                        .build();

                List<RoleType> roleTypes = Arrays.stream(line[6].split(","))
                        .map(String::trim)
                        .map(RoleType::valueOf) // 문자열 -> enum
                        .toList();

                roleTypes.forEach(roleType -> {
                    Role role = roleRepository.findByRoleType(roleType).orElseThrow(
                            () -> new RuntimeException("role 없음")
                    );

                    UserRole userRole = UserRole.builder()
                            .user(user)
                            .role(role)
                            .build();

                    user.getUserRoles().add(userRole);
                });

                userRepository.save(user);  // save( )마다 DB 접근 -> 10000번의 DB 통신
            }

            long endTime = System.currentTimeMillis();
            log.info("개별 save( ) 방식 완료. 총 10000건, 소요 시간: {}ms", (endTime - startTime));

        } catch (Exception e) {
            log.error("개별 save( ) 사용자 데이터 초기화 중 에러 발생: {}", e.getMessage());
        }
    }

    private void initUsersWithSaveAll() {
        if (userRepository.count() > 0) {
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info("saveAll( ) 방식으로 10000개의 사용자 데이터 로딩 시작");

        try {
            List<User> users = generateUserFromCsv();
            userRepository.saveAll(users);

            long endTime = System.currentTimeMillis();
            log.info("saveAll( ) 방식 완료. 총 10000건, 소요 시간: {}ms", (endTime - startTime));
        } catch (Exception e) {
            log.error("saveAll( ) 사용자 데이터 초기화 중 에러 발생: {}", e.getMessage());
        }
    }

    private void initUsersWithEntityManagerBatch() {
        if (userRepository.count() > 0) {
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info("EntityManager 배치 삽입 방식으로 10000개의 사용자 데이터 로딩 시작");

        try {
            List<User> users = generateUserFromCsv();
            int count = 0;
            for (User user : users) {
                entityManager.persist(user);
                count++;

                if (count % BATCH_SIZE == 0) {
                    entityManager.flush();  // DB에 Batch 전송
                    entityManager.clear();  // 영속성 컨텍스트 초기화

                    log.debug("Batch {} processed.", count / BATCH_SIZE);
                }
            }

            entityManager.flush();  // 마지막 남은 엔티티들 전송
            entityManager.clear();  // 마지막 영속성 컨텍스트 초기화

            long endTime = System.currentTimeMillis();
            log.info("EntityManager 배치 삽입 방식 완료. 총 10000건, 소요 시간: {}ms", (endTime - startTime));

        } catch (Exception e) {
            log.error("EntityManager 배치 사용자 데이터 초기화 중 에러 발생: {}", e.getMessage());
        }

    }



    private List<User> generateUserFromCsv() throws Exception {
        List<User> users = new ArrayList<>();

        InputStream inputStream = getClass().getResourceAsStream("/data/users.csv");
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        Role roleUser = roleRepository.findByRoleType(RoleType.ROLE_USER).orElseThrow(() -> new RuntimeException("role 없음"));
        Role roleCreator = roleRepository.findByRoleType(RoleType.ROLE_CREATOR).orElseThrow(() -> new RuntimeException("role 없음"));
        Map<RoleType, Role> roleMap = Map.of(
                RoleType.ROLE_USER, roleUser,
                RoleType.ROLE_CREATOR, roleCreator
        );

        CSVReader csvReader = new CSVReader(inputStreamReader);
        String [] line;
        while ((line = csvReader.readNext()) != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate birthDate = LocalDate.parse(line[3], formatter);

            User user = User.builder()
                    .email(line[0])
                    .password(passwordEncoder.encode(line[1]))
                    .nickname(line[2])
                    .birthDate(birthDate)
                    .pointBalance(100000)
                    .oAuthProvider(OAuthProvider.EMAIL)
                    .gender(Gender.valueOf(line[5]))
                    .termsAgreed(true)
                    .build();

            List<RoleType> roleTypes = Arrays.stream(line[6].split(","))
                    .map(String::trim)
                    .map(RoleType::valueOf) // 문자열 -> enum
                    .toList();

            roleTypes.forEach(roleType -> {

                UserRole userRole = UserRole.builder()
                        .user(user)
                        .role(roleMap.get(roleType))
                        .build();

                user.getUserRoles().add(userRole);
            });

            log.info("USER 데이터 ADD");
            users.add(user);
        }

        return users;
    }
}
