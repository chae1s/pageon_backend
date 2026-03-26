package com.pageon.backend.security;

import com.pageon.backend.entity.User;
import com.pageon.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        log.info("[Auth] Attempting to load user by username: {}", username);
        User user = userRepository.findByEmailAndDeletedAtIsNull(username).orElseThrow(
                () -> {
                    log.warn("[Auth] User not found by username: {}", username);
                    return new UsernameNotFoundException("사용자가 존재하지 않습니다.");
                }
        );

        log.info("[Auth] User found Successfully by username: {}", username);


        return new PrincipalUser(user);
    }
}
