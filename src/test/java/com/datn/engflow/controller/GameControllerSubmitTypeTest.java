package com.datn.engflow.controller;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v8: {@code POST /api/games/submit} took its payload as a raw Map and blind-cast
 * {@code sessionId} to String, {@code correctAnswers} to Number and each {@code answers}
 * element to Map. Any of those three holding a type the client chose threw
 * ClassCastException into the catch-all handler, i.e. a trivially reachable 500 that
 * also polluted the error log with a stack trace. Each shape is now a 400.
 */
@SpringBootTest
@Transactional
class GameControllerSubmitTypeTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private UserRepository userRepository;

    private MockMvc mvc;
    private UserPrincipal principal;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        User u = userRepository.findByEmail("user@gmail.com").orElseThrow();
        principal = new UserPrincipal(u);
    }

    private org.springframework.test.web.servlet.ResultActions submit(String json) throws Exception {
        return mvc.perform(post("/api/games/submit")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void correctAnswersAsObjectIs400Not500() throws Exception {
        submit("{\"sessionId\":\"zz-v8-sess\",\"answers\":null,\"correctAnswers\":{\"a\":1}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("correctAnswers phải là một số"));
    }

    @Test
    void correctAnswersAsStringIs400Not500() throws Exception {
        submit("{\"sessionId\":\"zz-v8-sess\",\"answers\":null,\"correctAnswers\":\"999\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("correctAnswers phải là một số"));
    }

    @Test
    void sessionIdAsNumberIs400Not500() throws Exception {
        submit("{\"sessionId\":12345,\"correctAnswers\":3}").andExpect(status().isBadRequest());
    }

    @Test
    void sessionIdAsObjectIs400Not500() throws Exception {
        submit("{\"sessionId\":{\"x\":1},\"correctAnswers\":3}").andExpect(status().isBadRequest());
    }

    @Test
    void answersNotAListIs400Not500() throws Exception {
        submit("{\"sessionId\":\"zz-v8-sess\",\"answers\":{\"a\":1},\"correctAnswers\":3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("answers phải là một danh sách"));
    }

    @Test
    void answersListOfScalarsIs400Not500() throws Exception {
        submit("{\"sessionId\":\"zz-v8-sess\",\"answers\":[\"x\",42],\"correctAnswers\":2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("mỗi phần tử answers phải là một đối tượng"));
    }

    @Test
    void missingSessionIdIsStill400() throws Exception {
        submit("{\"correctAnswers\":3}").andExpect(status().isBadRequest());
    }
}
