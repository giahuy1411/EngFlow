package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.SpeakingPrompt;
import com.datn.engflow.model.entity.SpeakingSubmission;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SpeakingSubmissionResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void fromExposesOnlySafeUserSummary() throws Exception {
        User user = User.builder()
                .id(42L)
                .username("learner")
                .email("learner@example.com")
                .passwordHash("bcrypt-secret")
                .fullName("Learner One")
                .avatarUrl("https://cdn.example.com/avatar.png")
                .build();
        SpeakingPrompt prompt = SpeakingPrompt.builder()
                .id(7L)
                .title("Introduce yourself")
                .build();
        SpeakingSubmission submission = SpeakingSubmission.builder()
                .id(99L)
                .user(user)
                .prompt(prompt)
                .videoUrl("https://media.example.com/submission.webm")
                .transcript("Hello, my name is Learner One.")
                .scorePronunciation(4)
                .scoreGrammar(5)
                .scoreVocabulary(4)
                .scoreFluency(4)
                .scoreTotal(17)
                .feedback("Legacy AI feedback")
                .score(new BigDecimal("8.5"))
                .adminFeedback("Good attempt")
                .privateNote("Internal moderation note")
                .gradedBy(User.builder().id(2L).fullName("Admin Grader").build())
                .gradedAt(LocalDateTime.of(2026, 7, 27, 1, 0))
                .submittedAt(null)
                .build();

        SpeakingSubmissionResponse response = SpeakingSubmissionResponse.from(submission);
        String json = objectMapper.writeValueAsString(response);

        assertThat(response.user().id()).isEqualTo(42L);
        assertThat(response.user().fullName()).isEqualTo("Learner One");
        assertThat(response.score()).isEqualByComparingTo("8.5");
        assertThat(response.adminFeedback()).isEqualTo("Good attempt");
        assertThat(json).contains("Learner One", "avatar.png", "Admin Grader", "Good attempt");
        assertThat(json).doesNotContain(
                "bcrypt-secret", "learner@example.com", "passwordHash", "token",
                "Internal moderation note", "privateNote");
    }
}
