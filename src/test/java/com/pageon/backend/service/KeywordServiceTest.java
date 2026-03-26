package com.pageon.backend.service;

import com.pageon.backend.entity.Category;
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


    @ParameterizedTest
    @ValueSource(strings = {
            "감성,드라마,로맨스",
            "스릴러,공포,추리",
            "판타지,이세계,마법"
    })
    @DisplayName("keyword가 DB에 있으면 저장하지 않고 반환")
    void separateKeywords_whenKeywordInDB_shouldAddSetList(String keyword) {
        // given
        String[] words = keyword.replaceAll("\\s", "").split(",");

        for (String word : words) {
            Keyword realKeyword = Keyword.builder()
                    .category(mockCategory)
                    .name(word)
                    .build();

            when(keywordRepository.findByName(word)).thenReturn(Optional.of(realKeyword));
        }
        //when
        List<Keyword> resultKeywords = keywordService.separateKeywords(keyword);
        
        // then
        assertEquals(words.length, resultKeywords.size());
        verify(keywordRepository, never()).save(any());
        
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "감성,드라마,로맨스",
            "스릴러,공포,추리",
            "판타지,이세계,마법"
    })
    @DisplayName("keyword가 DB에 없으면 DB에 저장")
    void separateKeywords_whenKeywordNotInDB_shouldCreateDB(String keyword) {
        // given
        String[] words = keyword.replaceAll("\\s", "").split(",");

        for (String word : words) {
            when(keywordRepository.findByName(word)).thenReturn(Optional.empty());
        }
        when(keywordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        //when
        List<Keyword> resultKeywords = keywordService.separateKeywords(keyword);

        // then
        assertEquals(words.length, resultKeywords.size());
        assertEquals(6L, resultKeywords.iterator().next().getCategory().getId());
        verify(keywordRepository, times(words.length)).save(any(Keyword.class));

    }
    
    @Test
    @DisplayName("null 입력 시 빈 리스트 반환")
    void separateKeywords_withNull_shouldReturnEmpty() {
        // when
        List<Keyword> result = keywordService.separateKeywords(null);
        
        // then
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(categoryRepository, never()).findById(any());
        
    }

    @Test
    @DisplayName("빈 문자열 입력 시 빈 리스트 반환")
    void separateKeywords_withBlank_shouldReturnEmpty() {
        // when
        List<Keyword> result = keywordService.separateKeywords("   ");

        // then
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(categoryRepository, never()).findById(any());

    }

    @Test
    @DisplayName("uncategorized 카테고리가 없으면 CustomException 발생")
    void separateKeywords_whenCategoryNotFound_shouldThrowCustomException() {
        // given
        when(categoryRepository.findById(6L)).thenReturn(Optional.empty());
        
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> keywordService.separateKeywords("키워드1, 키워드2")
        );
        
        // then
        assertEquals("카테고리가 존재하지 않습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        
    }


}