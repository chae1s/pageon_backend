package com.pageon.backend;

import com.pageon.backend.common.enums.RoleType;
import com.pageon.backend.security.JwtProvider;
import lombok.RequiredArgsConstructor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TokenGenerator {

    public static void main(String[] args) throws IOException {

        String refresh = "";
        String access = "";

        String directoryPath = "C:/Users/user/Desktop/project/workspace/pageOn/pageon_performance_test/scripts";
        File directory = new File(directoryPath);

        if (!directory.exists()) {
            directory.mkdir();
        }

        String fileName = "tokens.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(directory, fileName)));) {
            writer.write("userId,token\n");
            List<RoleType> roleTypes = new ArrayList<>();
            roleTypes.add(RoleType.ROLE_USER);


            JwtProvider jwtProvider = new JwtProvider(refresh, access);

            for (int i = 1; i <= 50000; i++) {
                String email = "user" + i + "@mail.com";
                String accessToken = jwtProvider.generateAccessToken((long)i -1, email, roleTypes);
                writer.write(email + ",\"" + accessToken + "\"\n");

                if (i % 10000 == 0) {
                    System.out.println(i + "개 생성 완료...");
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



        System.out.println("생성 완료! 파일명: " + fileName);
    }

}
