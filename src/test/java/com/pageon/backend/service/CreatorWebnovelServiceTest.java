/*
package com.pageon.backend.service;

import com.pageon.backend.common.enums.*;
import com.pageon.backend.dto.request.ContentCreateRequest;
import com.pageon.backend.dto.request.ContentDeleteRequest;
import com.pageon.backend.dto.request.ContentUpdateRequest;
import com.pageon.backend.dto.response.CreatorContentListResponse;
import com.pageon.backend.dto.response.CreatorWebnovelResponse;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import com.pageon.backend.security.PrincipalUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Transactional
@ActiveProfiles("test")
@DisplayName("CreatorWebnovelService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CreatorWebnovelServiceTest {
    @InjectMocks
    private CreatorWebnovelService webnovelService;
    @Mock
    private WebnovelRepository webnovelRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private PrincipalUser mockPrincipalUser;
    @Mock
    private FileUploadService fileUploadService;
    @Mock
    private CommonService commonService;
    @Mock
    private KeywordService keywordService;
    @Mock
    private ContentDeleteRepository contentDeleteRequestRepository;

    @BeforeEach
    void setUp() {
        webnovelRepository.deleteAll();
        Role roleUser = new Role("ROLE_USER");
        Role roleCreator = new Role("ROLE_CREATOR");

        when(roleRepository.findByRoleType(RoleType.ROLE_USER)).thenReturn(Optional.of(roleUser));
        when(roleRepository.findByRoleType(RoleType.ROLE_CREATOR)).thenReturn(Optional.of(roleCreator));
    }
    
    // 웹소설 저장
    @Test
    @DisplayName("로그인한 유저가 creator이고 content type이 webnovel일 때 제목, 설명, 웹소설, 키워드, 커버, 연재 요일 작성 시 생성 ")
    void createContent_withValidCreatorAndCorrectInput_shouldCreateWebnovel() {
        // given
        User user = createUser(1L);

        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);

        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);

        when(commonService.findCreatorByUser(user)).thenReturn(creator);

        MockMultipartFile mockFile =  new MockMultipartFile("file", "file".getBytes());

        ContentCreateRequest request = new ContentCreateRequest("웹소설 제목", "웹소설 설명", "하나,둘,셋,넷", mockFile, "MONDAY");
        List<Keyword> keywords = new ArrayList<>();

        doReturn(keywords).when(keywordService).separateKeywords("하나,둘,셋,넷");
        
        // 파일 업로드 했다고 가정
        doReturn("/webnovels/cover.png").when(fileUploadService).upload(any(), anyString());
        
        ArgumentCaptor<Webnovel> captor = ArgumentCaptor.forClass(Webnovel.class);
        
        //when
        webnovelService.createContent(mockPrincipalUser, request);
        
        // then
        verify(webnovelRepository).save(captor.capture());
        Webnovel webnovel = captor.getValue();
        
        assertEquals("웹소설 제목", webnovel.getTitle());
        assertEquals("웹소설 설명", webnovel.getDescription());
        assertEquals("/webnovels/cover.png", webnovel.getCover());
        assertEquals(SerialDay.MONDAY, webnovel.getSerialDay());
    }


    
    @Test
    @DisplayName("웹소설을 작성하려는 Creator의 contentType이 webnovel이 아니면 CustomException 발생")
    void createContent_withNotMatchContentType_shouldThrowCustomException() {
        // given
        User user = createUser(1L);

        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);

        Creator creator = createCreator(1L, user, ContentType.WEBTOON);
        when(commonService.findCreatorByUser(user)).thenReturn(creator);
        
        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            webnovelService.createContent(mockPrincipalUser, new ContentCreateRequest());
        });
        
        // then
        assertEquals("웹소설 업로드 권한이 없습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.NOT_CREATOR_OF_WEBNOVEL, ErrorCode.valueOf(exception.getErrorCode()));
        
    }

    
    @Test
    @DisplayName("웹소설 작성 후 cover file이 s3에 업로드가 되지 않았으면 CustomException 발생")
    void createContent_whenCoverUploadFails_shouldThrowCustomException() {
        // given
        User user = createUser(1L);

        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);

        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);

        when(commonService.findCreatorByUser(user)).thenReturn(creator);

        MockMultipartFile mockFile =  new MockMultipartFile("file", "file".getBytes());

        ContentCreateRequest request = new ContentCreateRequest("웹소설 제목", "웹소설 설명", "하나,둘,셋,넷", mockFile, "MONDAY");

        doThrow(new CustomException(ErrorCode.S3_UPLOAD_FAILED)).when(fileUploadService).upload(any(MockMultipartFile.class), any(String.class));
        
        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            webnovelService.createContent(mockPrincipalUser, request);
        });
        
        // then
        assertEquals("S3 업로드 중 오류가 발생했습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.S3_UPLOAD_FAILED, ErrorCode.valueOf(exception.getErrorCode()));
    }


    // 웹소설 1개 조회
    @Test
    @DisplayName("웹소설을 id로 조회했을 때 DB에 존재하고, 로그인한 유저가 작성자일 때 해당 작품을 return")
    void getContentById_whenUserIsCreator_shouldReturnWebnovel() {
        // given
        User user = createUser(1L);

        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);

        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);

        when(commonService.findCreatorByUser(user)).thenReturn(creator);

        Webnovel webnovel = Webnovel.builder()
                .id(1L)
                .title("테스트")
                .description("테스트")
                .cover("테스트")
                .creator(creator)
                .keywords(new ArrayList<>())
                .serialDay(SerialDay.MONDAY)
                .status(SeriesStatus.ONGOING)
                .build();

        when(webnovelRepository.findById(1L)).thenReturn(Optional.of(webnovel));

        //when

        CreatorWebnovelResponse result = webnovelService.getContentById(mockPrincipalUser, 1L);


        // then
        assertEquals(result.getTitle(), webnovel.getTitle());
        assertEquals(result.getDescription(), webnovel.getDescription());
        assertEquals(result.getStatus(), webnovel.getStatus());
        assertEquals(result.getSerialDay(), webnovel.getSerialDay());
    }

    @Test
    @DisplayName("웹소설을 id로 조회했을 때 DB에 존재하지 않으면 CustomException 발생")
    void getContentById_whenWebnovelNotFound_shouldThrowCustomException() {
        // given
        User user = createUser(1L);

        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);

        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);

        when(commonService.findCreatorByUser(user)).thenReturn(creator);

        Long id = 1L;

        when(webnovelRepository.findById(id)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            webnovelService.getContentById(mockPrincipalUser, id);
        });

        // then
        assertEquals("존재하지 않는 웹소설입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.WEBNOVEL_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("DB에서 id로 조회한 웹소설의 작성자가 로그인한 유저가 아니면 CustomException 발생")
    void getContentById_whenInvalidCreator_shouldThrowCustomException() {
        // given
        User loggedUser = createUser(1L);

        User createUser = createUser(2L);

        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(loggedUser);

        Creator loggedCreator = createCreator(1L, loggedUser, ContentType.WEBNOVEL);
        Creator otherCreator = createCreator(2L, createUser, ContentType.WEBNOVEL);

        when(commonService.findCreatorByUser(loggedUser)).thenReturn(loggedCreator);

        Webnovel webnovel = Webnovel.builder()
                .id(1L)
                .title("테스트")
                .description("테스트")
                .cover("테스트")
                .creator(otherCreator)
                .keywords(new ArrayList<>())
                .serialDay(SerialDay.MONDAY)
                .status(SeriesStatus.ONGOING)
                .build();

        when(webnovelRepository.findById(1L)).thenReturn(Optional.of(webnovel));
        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            webnovelService.getContentById(mockPrincipalUser, 1L);
        });

        // then
        assertEquals("해당 콘텐츠의 작성자가 아닙니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.CREATOR_UNAUTHORIZED_ACCESS, ErrorCode.valueOf(exception.getErrorCode()));
    }
    
    // 작성한 웹소설 목록 리턴
    @Test
    @DisplayName("로그인한 작가가 자신이 작성한 웹소설의 목록을 조회")
    void getMyContents_whenValidCreator_shouldReturnWebnovelList() {
        // given
        User user = createUser(1L);

        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);

        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);

        when(commonService.findCreatorByUser(user)).thenReturn(creator);

        Webnovel webnovel = Webnovel.builder()
                .id(1L)
                .title("테스트")
                .description("테스트")
                .cover("테스트")
                .creator(creator)
                .keywords(new ArrayList<>())
                .serialDay(SerialDay.MONDAY)
                .status(SeriesStatus.ONGOING)
                .build();

        List<Webnovel> webnovelList = new ArrayList<>();
        webnovelList.add(webnovel);

        when(webnovelRepository.findByCreator(creator)).thenReturn(webnovelList);
        //when
        List<CreatorContentListResponse> result = webnovelService.getMyContents(mockPrincipalUser);
        
        // then
        assertEquals(result.size(), webnovelList.size());
        assertEquals(result.get(0).getTitle(), webnovel.getTitle());
        
    }

    @Test
    @DisplayName("로그인한 작가가 자신이 작성한 웹소설 정보를 수정")
    void updateContent_whenValidCreatorAndCorrectInput_shouldUpdateWebnovel() {
        // given
        User user = createUser(1L);

        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);

        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);

        when(commonService.findCreatorByUser(user)).thenReturn(creator);
        Webnovel webnovel = Webnovel.builder()
                .id(1L)
                .title("테스트")
                .description("테스트")
                .creator(creator)
                .keywords(createKeywords("하나,둘,셋,넷"))
                .serialDay(SerialDay.MONDAY)
                .status(SeriesStatus.ONGOING)
                .build();

        when(webnovelRepository.findById(1L)).thenReturn(Optional.of(webnovel));
        String newTitle = "새 제목";
        String newDescription = "새 내용";
        String newSerialDay = "TUESDAY";
        String newKeywords = "하나,둘,셋";
        String newStatus = "REST";

        List<Keyword> newKeywordList = createKeywords(newKeywords);
        doReturn(newKeywordList).when(keywordService).separateKeywords(newKeywords);

        ContentUpdateRequest request = new ContentUpdateRequest(newTitle, newDescription, newKeywords, null, newSerialDay, newStatus);

        //when
        webnovelService.updateContent(mockPrincipalUser, 1L, request);

        // then
        assertEquals(newTitle, webnovel.getTitle());
        assertEquals(newDescription, webnovel.getDescription());
        assertEquals(3, webnovel.getKeywords().size());
        assertEquals(SerialDay.valueOf(newSerialDay), webnovel.getSerialDay());

    }

    @Test
    @DisplayName("커버 이미지 변경 시 기존 s3 이미지 버킷에서 제거 후 새로운 파일 업로드")
    void updateContent_whenWebnovelCoverUpdate_shouldDeleteOldFileAndUploadNewFile() {
        // given
        User user = createUser(1L);
        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);
        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);
        when(commonService.findCreatorByUser(user)).thenReturn(creator);

        Webnovel webnovel = Webnovel.builder()
                .id(1L)
                .title("테스트")
                .description("테스트")
                .creator(creator)
                .keywords(createKeywords("하나,둘,셋,넷"))
                .cover("https://gdsdgtehh.cloudfront.net/filename.png")
                .serialDay(SerialDay.MONDAY)
                .status(SeriesStatus.ONGOING)
                .build();

        String oldCover = webnovel.getCover();

        MockMultipartFile mockFile =  new MockMultipartFile("newFile.png", "file".getBytes());
        when(webnovelRepository.findById(1L)).thenReturn(Optional.of(webnovel));

        ContentUpdateRequest request = new ContentUpdateRequest(null, null, null, mockFile, null, null);

        doNothing().when(fileUploadService).deleteFile(oldCover);
        doReturn("newUrl").when(fileUploadService).upload(mockFile, String.format("webnovels/%d", webnovel.getId()));
        //when
        webnovelService.updateContent(mockPrincipalUser, 1L, request);

        // then
        verify(fileUploadService).deleteFile(oldCover);
        verify(fileUploadService).upload(mockFile, String.format("webnovels/%d", webnovel.getId()));
        assertEquals("newUrl", webnovel.getCover());


    }

    @Test
    @DisplayName("DB에 수정하려는 작품이 없을 때 CustomException 발생")
    void updateContent_withInvalidWebnovelId_shouldThrowCustomException() {
        // given
        User user = createUser(1L);
        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);
        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);
        when(commonService.findCreatorByUser(user)).thenReturn(creator);

        when(webnovelRepository.findById(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            webnovelService.updateContent(mockPrincipalUser, 1L, new ContentUpdateRequest());
        });

        // then
        assertEquals("존재하지 않는 웹소설입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.WEBNOVEL_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
    }

    @Test
    @DisplayName("로그인한 작가가 자신이 작성한 작품을 삭제 요청")
    void deleteRequestContent_withValidCreator_shouldRequestDeleteWebnovel() {
        // given
        User user = createUser(1L);
        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);
        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);
        when(commonService.findCreatorByUser(user)).thenReturn(creator);

        Webnovel webnovel = Webnovel.builder()
                .id(1L)
                .title("테스트")
                .description("테스트")
                .creator(creator)
                .keywords(createKeywords("하나,둘,셋,넷"))
                .serialDay(SerialDay.MONDAY)
                .status(SeriesStatus.ONGOING)
                .build();

        when(webnovelRepository.findById(1L)).thenReturn(Optional.of(webnovel));

        ContentDelete contentDeleteRequest = ContentDelete.builder()
                .id(1L)
                .contentId(1L)
                .creator(creator)
                .contentType(ContentType.WEBNOVEL)
                .reason("그냥")
                .deleteStatus(DeleteStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        ArgumentCaptor<ContentDelete> requestCaptor = ArgumentCaptor.forClass(ContentDelete.class);
        ContentDeleteRequest deleteRequest = new ContentDeleteRequest("그냥");
        //when
        webnovelService.deleteRequestContent(mockPrincipalUser, 1L, deleteRequest);

        // then
        verify(contentDeleteRequestRepository).save(requestCaptor.capture());
        ContentDelete request = requestCaptor.getValue();
        assertEquals(webnovel.getId(), request.getContentId());
        assertEquals(creator.getPenName(), request.getCreator().getPenName());

    }

    @Test
    @DisplayName("DB에 수정하려는 작품이 없을 때 CustomException 발생")
    void deleteRequestContent_withInvalidWebnovelId_shouldThrowCustomException() {
        // given
        User user = createUser(1L);
        when(commonService.findUserByEmail(mockPrincipalUser.getUsername())).thenReturn(user);
        Creator creator = createCreator(1L, user, ContentType.WEBNOVEL);
        when(commonService.findCreatorByUser(user)).thenReturn(creator);

        when(webnovelRepository.findById(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            webnovelService.deleteRequestContent(mockPrincipalUser, 1L, new ContentDeleteRequest());
        });

        // then
        assertEquals("존재하지 않는 웹소설입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.WEBNOVEL_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
    }


    // role에 creator가 포함되어 있는 유저를 return
    private User createUser(Long id) {

        User user = User.builder()
                .id(id)
                .email("test@mail.com")
                .nickname("테스트")
                .userRoles(new ArrayList<>())
                .deleted(false)
                .isPhoneVerified(true)
                .build();

        Role roleUser = roleRepository.findByRoleType(RoleType.ROLE_USER).orElseThrow(() -> new CustomException(ErrorCode.ROLE_NOT_FOUND));

        UserRole userRole = UserRole.builder()
                .role(roleUser)
                .user(user)
                .build();

        user.getUserRoles().add(userRole);


        Role roleCreator = roleRepository.findByRoleType(RoleType.ROLE_CREATOR).orElseThrow(() -> new CustomException(ErrorCode.ROLE_NOT_FOUND));

        UserRole createrRole = UserRole.builder()
                .role(roleCreator)
                .user(user)
                .build();

        user.getUserRoles().add(createrRole);


        return user;
    }

    // user가 포함되어 있는 creator를 return
    private Creator createCreator(Long id, User user, ContentType contentType) {
        Creator creator = Creator.builder()
                .id(id)
                .penName("필명")
                .user(user)
                .contentType(contentType)
                .agreedToAiPolicy(true)
                .aiPolicyAgreedAt(LocalDateTime.now())
                .build();

        return creator;
    }

    private List<Keyword> createKeywords(String s) {
        Category category = Category.builder()
                .id(1L)
                .name("카테고리")
                .build();

        List<Keyword> keywords = new ArrayList<>();
        String[] words = s.replaceAll("\\s", "").split(",");
        for (int i = 0; i < words.length; i++) {
            Keyword keyword = Keyword.builder()
                    .id(i + 1L)
                    .category(category)
                    .name(words[i])
                    .build();

            keywords.add(keyword);
        }

        return keywords;
    }


}*/
