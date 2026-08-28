package com.datn.engflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for P1: GET /api/vocabulary returned 405 Method Not Allowed
 * because VocabularyController only exposed /search and POST. The base path
 * now publishes a paginated GET so the bare endpoint resolves.
 *
 * Note: Spring Boot 4 no longer auto-registers a MockMvc bean from
 * @SpringBootTest, so the MockMvc instance is built from the live
 * WebApplicationContext to keep the full security filter chain in play.
 */
 @SpringBootTest
@Transactional
class VocabularyControllerEndpointTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void baseGetReturns200AndContent() throws Exception {
        long before = vocabularyRepository.count();

        var req = MockMvcRequestBuilders.get("/api/vocabulary");

        mockMvc.perform(req)
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").exists());
    }

    @Test
    void searchEndpointStillWorks() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/vocabulary")
                        .param("keyword", "abc"))
                .andExpect(status().isOk());
    }

    @Test
    void postWithoutAuthIsUnauthorized() throws Exception {
        VocabularyRequest req = new VocabularyRequest();
        req.setWord("test");
        req.setMeaning("test");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/vocabulary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
