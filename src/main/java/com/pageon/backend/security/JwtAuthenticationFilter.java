package com.pageon.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pageon.backend.common.enums.RoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = jwtProvider.resolveToken(request);

        try {
            if (token != null) {
                Claims claims = jwtProvider.validateAndGetClaims(token);
                Long userId = claims.get("userId", Long.class);
                String username = claims.get("email", String.class);
                List<RoleType> roles = jwtProvider.getRolesFromClaims(claims);

                PrincipalUser principalUser = new PrincipalUser(userId, username, roles);


                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principalUser, null, principalUser.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (ExpiredJwtException e) {
            ObjectMapper objectMapper = new ObjectMapper();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, String> error = new HashMap<>();
            error.put("message", "access token expired");
            error.put("code", "401");
            error.put("status", "UNAUTHORIZED");

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(error));

            return;
        }

        filterChain.doFilter(request, response);


    }

}
