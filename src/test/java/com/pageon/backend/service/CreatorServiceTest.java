/*
package com.pageon.backend.service;

import com.pageon.backend.common.enums.RoleType;
import com.pageon.backend.dto.request.RegisterCreatorRequest;
import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.Role;
import com.pageon.backend.entity.UserRole;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.CreatorRepository;
import com.pageon.backend.repository.RoleRepository;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.security.PrincipalUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Transactional
@ActiveProfiles("test")
@DisplayName("CreatorService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CreatorServiceTest {
    @InjectMocks
    private CreatorService creatorService;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    private PrincipalUser mockPrincipalUser;
    @Mock
    private CommonService commonService;

    @BeforeEach
    void setUp() {
        creatorRepository.deleteAll();

        mockPrincipalUser = mock(PrincipalUser.class);
    }

    @Test
    @DisplayName("로그인한 유저가 올바른 정보를 입력하면 창작자 등록 성공")
    void registerCreator_withValidInput_shouldSucceed() {
        // given
        String email = "test@mail.com";
        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .nickname("nickname")
                .userRoles(new ArrayList<>())
                .build();

        Role role = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_USER)
                .build();

        Role creatorRole = Role.builder()
                .id(2L)
                .roleType(RoleType.ROLE_CREATOR)
                .build();

        UserRole userRole = new UserRole(1L, user, role);
        user.getUserRoles().add(userRole);

        when(mockPrincipalUser.getUsername()).thenReturn(email);
        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);
        when(roleRepository.findByRoleType(RoleType.ROLE_CREATOR)).thenReturn(Optional.of(creatorRole));

        RegisterCreatorRequest creatorRequest = new RegisterCreatorRequest("필명", "WEBNOVEL", true);
        ArgumentCaptor<Creator> creatorCaptor = ArgumentCaptor.forClass(Creator.class);

        //when
        creatorService.registerCreator(mockPrincipalUser, creatorRequest);

        // then
        verify(creatorRepository).save(creatorCaptor.capture());
        Creator savedCreator = creatorCaptor.getValue();

        assertEquals(RoleType.ROLE_CREATOR, savedCreator.getUser().getUserRoles().get(1).getRole().getRoleType());
        assertEquals("필명", savedCreator.getPenName());
        assertTrue(savedCreator.getIsActive());
    }


    @Test
    @DisplayName("로그인한 유저가 이미 creator 권한이 있을 경우 CustomException 발생")
    void registerCreator_whenUserAlreadyHasCreatorRole_shouldThrowException() {
        // given
        String email = "test@mail.com";
        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .nickname("nickname")
                .build();

        Role creatorRole = Role.builder()
                .id(2L)
                .roleType(RoleType.ROLE_CREATOR)
                .build();

        when(mockPrincipalUser.getUsername()).thenReturn(email);
        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);
        when(roleRepository.findByRoleType(RoleType.ROLE_CREATOR)).thenReturn(Optional.of(creatorRole));
        when(creatorRepository.findByUser(user)).thenThrow(new CustomException(ErrorCode.ALREADY_HAS_CREATOR_ROLE));

        RegisterCreatorRequest creatorRequest = new RegisterCreatorRequest("필명", "WEBNOVEL", true);
        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            creatorService.registerCreator(mockPrincipalUser, creatorRequest);
        });

        // then
        assertEquals("이미 창작자 권한이 존재합니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.ALREADY_HAS_CREATOR_ROLE, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("창작자 등록을 할 때 약관에 동의하지 않으면 CustomException 발생")
    void registerCreator_withoutAgreeingToAiPolicy_shouldThrowCustomException() {
        // given
        String email = "test@mail.com";
        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .nickname("nickname")
                .userRoles(new ArrayList<>())
                .build();

        Role role = Role.builder()
                .id(1L)
                .roleType(RoleType.ROLE_USER)
                .build();

        Role creatorRole = Role.builder()
                .id(2L)
                .roleType(RoleType.ROLE_CREATOR)
                .build();
        UserRole userRole = new UserRole(1L, user, role);
        user.getUserRoles().add(userRole);

        when(mockPrincipalUser.getUsername()).thenReturn(email);
        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);
        when(roleRepository.findByRoleType(RoleType.ROLE_CREATOR)).thenReturn(Optional.of(creatorRole));

        RegisterCreatorRequest creatorRequest = new RegisterCreatorRequest("필명", "WEBNOVEL", false);
        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            creatorService.registerCreator(mockPrincipalUser, creatorRequest);
        });

        // then
        assertEquals("AI 콘텐츠 등록 약관에 동의하지 않았습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.AI_POLICY_NOT_AGREED, ErrorCode.valueOf(exception.getErrorCode()));

    }


}*/
