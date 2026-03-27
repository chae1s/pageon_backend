package com.pageon.backend.service;

import com.pageon.backend.entity.Category;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.ContentKeyword;
import com.pageon.backend.entity.Keyword;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.CategoryRepository;
import com.pageon.backend.repository.KeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DisplayName("KeywordService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {
    @InjectMocks
    private KeywordService keywordService;
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private CategoryRepository categoryRepository;
    private Category mockCategory;

    @BeforeEach
    void setUp() {
        mockCategory = Category.builder().id(6L).name("uncategorized").build();

        lenient().when(categoryRepository.findById(6L)).thenReturn(Optional.of(mockCategory));
    }

    @Test
    @DisplayName("Content에 ContentKeyword가 정상 추가된다.")
    void registerContentKeyword_withValidInput_shouldAddContentKeywords() {
        // given
        Content content = mock(Content.class);
        List<ContentKeyword> contentKeywords = new ArrayList<>();
        when(content.getContentKeywords()).thenReturn(contentKeywords);

        Keyword keyword = Keyword.builder().name("SF").category(mockCategory).build();
        when(keywordRepository.findByName("SF")).thenReturn(Optional.of(keyword));

        //when
        keywordService.registerContentKeyword(content, "SF");

        // then
        assertEquals(1, contentKeywords.size());
        assertEquals("SF", contentKeywords.get(0).getKeyword().getName());

    }


    @ParameterizedTest
    @ValueSource(strings = {
            "감성,드라마,로맨스",
            "스릴러,공포,추리",
            "판타지,이세계,마법"
    })
    @DisplayName("keyword가 DB에 있으면 저장하지 않고 반환")
    void registerContentKeyword_whenKeywordInDB_shouldAddSetList(String keyword) {
        // given
        Content content = mock(Content.class);
        List<ContentKeyword> contentKeywords = new ArrayList<>();
        when(content.getContentKeywords()).thenReturn(contentKeywords);

        String[] words = keyword.replaceAll("\\s", "").split(",");

        for (String word : words) {
            Keyword realKeyword = Keyword.builder()
                    .category(mockCategory)
                    .name(word)
                    .build();

            when(keywordRepository.findByName(word)).thenReturn(Optional.of(realKeyword));
        }
        //when
        keywordService.registerContentKeyword(content, keyword);

        // then
        assertEquals(words.length, contentKeywords.size());
        verify(keywordRepository, never()).save(any());

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "감성,드라마,로맨스",
            "스릴러,공포,추리",
            "판타지,이세계,마법"
    })
    @DisplayName("keyword가 DB에 없으면 DB에 저장")
    void registerContentKeyword_whenKeywordNotInDB_shouldCreateDB(String keyword) {
        // given
        Content content = mock(Content.class);
        List<ContentKeyword> contentKeywords = new ArrayList<>();
        when(content.getContentKeywords()).thenReturn(contentKeywords);

        String[] words = keyword.replaceAll("\\s", "").split(",");

        for (String word : words) {
            when(keywordRepository.findByName(word)).thenReturn(Optional.empty());
        }
        when(keywordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        //when
        keywordService.registerContentKeyword(content, keyword);

        // then
        assertEquals(words.length, contentKeywords.size());
        assertEquals(6L, contentKeywords.get(0).getKeyword().getCategory().getId());
        verify(keywordRepository, times(words.length)).save(any(Keyword.class));

    }

    @Test
    @DisplayName("null 입력 시 ContentKeyword가 추가되지 않는다")
    void registerContentKeyword_withNull_shouldNotAddKeywords() {
        // given
        Content content = mock(Content.class);
        List<ContentKeyword> contentKeywords = new ArrayList<>();

        // when
        keywordService.registerContentKeyword(content, null);

        // then

        assertTrue(contentKeywords.isEmpty());
        verify(categoryRepository, never()).findById(any());

    }

    @Test
    @DisplayName("빈 문자열 입력 시 ContentKeyword가 추가되지 않는다")
    void registerContentKeyword_withBlank_shouldNotAddKeywords() {
        // given
        Content content = mock(Content.class);
        List<ContentKeyword> contentKeywords = new ArrayList<>();


        // when
        keywordService.registerContentKeyword(content, "    ");

        // then
        assertTrue(contentKeywords.isEmpty());
        verify(categoryRepository, never()).findById(any());

    }

    @Test
    @DisplayName("중복된 키워드는 한 번만 추가")
    void registerContentKeyword_withDuplicateKeyword_shouldAddOnce() {
        // given
        Content content = mock(Content.class);
        List<ContentKeyword> contentKeywords = new ArrayList<>();
        when(content.getContentKeywords()).thenReturn(contentKeywords);

        Keyword keyword = Keyword.builder().name("SF").category(mockCategory).build();
        when(keywordRepository.findByName("SF")).thenReturn(Optional.of(keyword));

        // when
        keywordService.registerContentKeyword(content, "SF,SF,SF");

        // then
        assertEquals(1, contentKeywords.size());
        verify(keywordRepository, times(1)).findByName("SF");
        verify(keywordRepository, never()).save(any());

    }

    @Test
    @DisplayName("uncategorized 카테고리가 없으면 CustomException 발생")
    void registerContentKeyword_whenCategoryNotFound_shouldThrowCustomException() {
        // given
        Content content = mock(Content.class);
        List<ContentKeyword> contentKeywords = new ArrayList<>();

        when(categoryRepository.findById(6L)).thenReturn(Optional.empty());
        
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> keywordService.registerContentKeyword(content, "키워드1, 키워드2")
        );
        
        // then
        assertEquals("카테고리가 존재하지 않습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        
    }

    private ContentKeyword mockContentKeyword(String keywordName) {
        Keyword keyword = Keyword.builder().name(keywordName).category(mockCategory).build();
        return ContentKeyword.builder().keyword(keyword).build();
    }

    @Test
    @DisplayName("키워드가 변경되면 contentKeyword 업데이트")
    void updateContentKeywords_whenNewKeywords_shouldUpdate() {
        // given
        Content content = mock(Content.class);
        List<ContentKeyword> existingKeywords = new ArrayList<>(
                List.of(mockContentKeyword("SF"), mockContentKeyword("AI"))
        );
        when(content.getContentKeywords()).thenReturn(existingKeywords);
        when(keywordRepository.findByName("판타지")).thenReturn(
                Optional.of(Keyword.builder().name("판타지").build()));
        when(keywordRepository.findByName("로맨스")).thenReturn(
                Optional.of(Keyword.builder().name("로맨스").build()));


        //when
        keywordService.updateContentKeyword(content, "판타지,로맨스");

        // then
        assertEquals(2, content.getContentKeywords().size());
        assertEquals("판타지", existingKeywords.get(0).getKeyword().getName());
        assertEquals("로맨스", existingKeywords.get(1).getKeyword().getName());
    }

    @Test
    @DisplayName("키워드 일부 변경 시 전체 교체됨")
    void updateContentKeyword_whenPartiallyChanged_shouldReplaceAll() {
        // given
        List<ContentKeyword> existingKeywords = new ArrayList<>(
                List.of(mockContentKeyword("SF"), mockContentKeyword("AI"))
        );
        Content content = mock(Content.class);
        when(content.getContentKeywords()).thenReturn(existingKeywords);

        when(keywordRepository.findByName("SF")).thenReturn(
                Optional.of(Keyword.builder().name("SF").build()));
        when(keywordRepository.findByName("판타지")).thenReturn(
                Optional.of(Keyword.builder().name("판타지").build()));

        // when
        keywordService.updateContentKeyword(content, "SF,판타지");

        // then
        assertEquals(2, existingKeywords.size());
        List<String> names = existingKeywords.stream()
                .map(ck -> ck.getKeyword().getName())
                .toList();
        assertTrue(names.contains("SF"));
        assertTrue(names.contains("판타지"));
        assertFalse(names.contains("AI"));
    }

    @Test
    @DisplayName("키워드가 변경되지 않으면 ContentKeyword 업데이트 안 함")
    void updateContentKeyword_whenKeywordsNotChanged_shouldNotUpdate() {
        // given
        List<ContentKeyword> existingKeywords = new ArrayList<>(
                List.of(mockContentKeyword("SF"), mockContentKeyword("AI"))
        );
        Content content = mock(Content.class);
        when(content.getContentKeywords()).thenReturn(existingKeywords);

        when(keywordRepository.findByName("SF")).thenReturn(
                Optional.of(Keyword.builder().name("SF").build()));
        when(keywordRepository.findByName("AI")).thenReturn(
                Optional.of(Keyword.builder().name("AI").build()));

        // when
        keywordService.updateContentKeyword(content, "SF,AI");

        // then
        assertEquals(2, existingKeywords.size());
        verify(keywordRepository, never()).save(any());
    }

    @Test
    @DisplayName("null 키워드 라인이면 키워드 업데이트 안 함")
    void updateContentKeyword_withNull_shouldNotUpdate() {
        // given
        Content content = mock(Content.class);

        // when
        keywordService.updateContentKeyword(content, null);

        // then
        verify(content, never()).getContentKeywords();
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("빈 키워드 라인이면 키워드 업데이트 안 함")
    void updateContentKeyword_withBlank_shouldNotUpdate() {
        // given
        Content content = mock(Content.class);

        // when
        keywordService.updateContentKeyword(content, "   ");

        // then
        verify(content, never()).getContentKeywords();
        verify(categoryRepository, never()).findById(any());
    }


}