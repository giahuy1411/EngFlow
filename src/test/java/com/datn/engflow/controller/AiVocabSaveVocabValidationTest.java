package com.datn.engflow.controller;

import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * POST /api/ai/save-vocab phải thực thi ràng buộc đã khai báo trên
 * {@link VocabularyRequest} (@NotBlank word/meaning, @Size). Trước đây controller
 * thiếu kích hoạt validate nên payload rác rơi thẳng xuống DB: word trống thành
 * DataIntegrityViolation (500 mờ mịt), meaning trống thì lưu yên lặng — từ vựng
 * bảng dùng chung nên một dòng rác làm hỏng dữ liệu cho mọi user.
 */
@SpringBootTest
@Transactional
@WithMockUser
class AiVocabSaveVocabValidationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.datn.engflow.repository.VocabularyRepository vocabularyRepository;

    @Autowired
    private com.datn.engflow.repository.DeckWordRepository deckWordRepository;

    @Autowired
    private com.datn.engflow.repository.DeckRepository deckRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * The controller reads the caller's id from {@code @AuthenticationPrincipal UserPrincipal}.
     * {@code @WithMockUser} supplies a plain Spring user, which is not a UserPrincipal, so the
     * deck-linking branch would be skipped. This mirrors the pattern used by the other
     * audit security tests (e.g. AuditV9DraftLessonGradeGuardTest).
     */
    private org.springframework.test.web.servlet.request.RequestPostProcessor asRealUser() {
        var user = userRepository.findByEmail("user@gmail.com").orElseThrow();
        var principal = new com.datn.engflow.security.UserPrincipal(user);
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return request -> {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    private String json(List<VocabularyRequest> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void saveVocab_blankWord_returns400Validation() throws Exception {
        VocabularyRequest bad = VocabularyRequest.builder().word("").meaning("nghia hop le").build();

        mockMvc.perform(post("/api/ai/save-vocab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(List.of(bad))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void saveVocab_blankMeaning_returns400InsteadOfSavingJunk() throws Exception {
        // meaning NULL/trống hợp lệ ở tầng DB (column cho phép) → nếu lọt qua
        // validate là im lặng lưu dữ liệu rác: bug nguy hiểm nhất của cặp test này.
        VocabularyRequest bad = VocabularyRequest.builder().word("cat").meaning("   ").build();

        mockMvc.perform(post("/api/ai/save-vocab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(List.of(bad))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveVocab_validPayload_stillReturns200() throws Exception {
        VocabularyRequest good = VocabularyRequest.builder()
                .word("journey").meaning("chuyến đi").build();

        mockMvc.perform(post("/api/ai/save-vocab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(List.of(good))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].word").value("journey"));
    }

    /** audit-v7 F60: batch quá 50 từ phải bị chặn ở controller (400), không xuống service. */
    @Test
    void saveVocab_oversizedBatch_returns400() throws Exception {
        List<VocabularyRequest> many = new java.util.ArrayList<>();
        for (int i = 0; i < 51; i++) {
            many.add(VocabularyRequest.builder().word("w" + i).meaning("nghia " + i).build());
        }

        mockMvc.perform(post("/api/ai/save-vocab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(many)))
                .andExpect(status().isBadRequest());
    }

    /** audit-v7 F60: payload rỗng [] trước đây lọt xuống batch save (200 vô nghĩa). */
    @Test
    void saveVocab_emptyList_returns400() throws Exception {
        mockMvc.perform(post("/api/ai/save-vocab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(List.of())))
                .andExpect(status().isBadRequest());
    }

    /**
     * audit-v11 F145: khi có deckId, từ vừa lưu PHẢI được gắn vào deck đó — nếu không thì
     * người dùng trả 1 lượt quota, thấy báo "đã lưu", và không bao giờ mở lại được từ.
     *
     * Test này kiểm đúng cái đã hỏng: response 200 + header nói đã gắn deck, và hàng trong
     * deck_words thực sự tồn tại (đọc lại bằng repository, không tin header).
     */
    @Test
    void saveVocab_withDeckId_linksEachWordIntoThatDeck() throws Exception {
        VocabularyRequest good = VocabularyRequest.builder()
                .word("f145linkword").meaning("tu kiem thu F145").build();
        var owner = userRepository.findByEmail("user@gmail.com").orElseThrow();
        Long deckId = deckRepository.save(com.datn.engflow.model.entity.Deck.builder()
                .name("F145 TEST DECK").owner(owner).isPublic(false).build()).getId();

        mockMvc.perform(post("/api/ai/save-vocab")
                        .param("deckId", String.valueOf(deckId))
                        .with(asRealUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(List.of(good))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-AI-Linked-To-Deck", "true"))
                .andExpect(jsonPath("$[0].word").value("f145linkword"));

        // the word must now be reachable THROUGH the deck
        Long vocabId = vocabularyRepository.findByWordContainingIgnoreCase("f145linkword")
                .stream().findFirst().orElseThrow().getId();
        assertThat(deckWordRepository.existsByDeckIdAndVocabularyId(deckId, vocabId)).isTrue();
    }

    /** Without a deck the words still save (contract unchanged) but the header says so. */
    @Test
    void saveVocab_withoutDeckId_reportsNotLinked() throws Exception {
        VocabularyRequest good = VocabularyRequest.builder()
                .word("f145orphanword").meaning("tu khong deck").build();

        mockMvc.perform(post("/api/ai/save-vocab")
                        .with(asRealUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(List.of(good))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-AI-Linked-To-Deck", "false"));
    }
}
