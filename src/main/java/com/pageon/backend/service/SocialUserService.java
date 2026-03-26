package com.pageon.backend.service;

import com.pageon.backend.dto.oauth.OAuthUserInfoResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class SocialUserService {
    private final RoleService roleService;
    private final UserRepository userRepository;

    @Transactional
    public User signupSocial(OAuthUserInfoResponse response) {
        User user = User.builder()
                .email(response.getEmail())
                .nickname(generateRandomNickname())
                .oAuthProvider(response.getOAuthProvider())
                .providerId(response.getProviderId())
                .termsAgreed(true)
                .build();

        roleService.assignDefaultRole(user);

        userRepository.save(user);

        return user;
    }

    private String generateRandom() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        int randomLength = random.nextInt(5) + 6;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < randomLength; i++) {
            int index = random.nextInt(alphabet.length());
            sb.append(alphabet.charAt(index));
        }

        return sb.toString();
    }

    private String generateRandomNickname() {
        String nickname;
        do {
            nickname = generateRandom();
        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }
}
