package com.datn.engflow.controller;

import com.datn.engflow.model.dto.VocabularyRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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
}
