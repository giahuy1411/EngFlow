package com.datn.engflow.config;

import com.datn.engflow.model.entity.SpeakingPrompt;
import com.datn.engflow.model.entity.SpeakingPromptMode;
import com.datn.engflow.repository.SpeakingPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds READ_ALOUD prompts with reference text so the automatic assessment pipeline
 * has scripted material to align transcripts against. Idempotent: runs only when no
 * READ_ALOUD prompt exists yet.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
/**
 * class SpeakingReadAloudPromptSeeder.
 */
public class SpeakingReadAloudPromptSeeder implements ApplicationRunner {

    private static final List<SpeakingPrompt> SEEDS = List.of(
            SpeakingPrompt.builder()
                    .title("Daily routine - read aloud")
                    .description("Đọc to đoạn văn về thói quen hằng ngày, chú ý thì hiện tại đơn.")
                    .prompt("Read the passage aloud in a clear, natural voice.")
                    .mode(SpeakingPromptMode.READ_ALOUD)
                    .referenceText("I usually wake up at seven o'clock in the morning. "
                            + "After that, I have breakfast with my family and walk to school. "
                            + "In the evening, I do my homework and read a short book before bed.")
                    .level("A2")
                    .category("Daily life")
                    .isPremium(false)
                    .isPublished(true)
                    .orderIndex(1)
                    .maxDurationSeconds(30)
                    .attemptLimit(10)
                    .build(),
            SpeakingPrompt.builder()
                    .title("Ordering coffee - read aloud")
                    .description("Đọc to đoạn hội thoại ngắn khi gọi đồ uống tại quán cà phê.")
                    .prompt("Read the dialogue aloud with natural intonation.")
                    .mode(SpeakingPromptMode.READ_ALOUD)
                    .referenceText("Good morning, can I have a large cappuccino with oat milk, please? "
                            + "Could you also add a blueberry muffin to that order? "
                            + "Thank you very much, I will pay by card.")
                    .level("A2")
                    .category("Travel")
                    .isPremium(false)
                    .isPublished(true)
                    .orderIndex(2)
                    .maxDurationSeconds(30)
                    .attemptLimit(10)
                    .build(),
            SpeakingPrompt.builder()
                    .title("Job interview intro - read aloud")
                    .description("Đọc to đoạn tự giới thiệu bản thân trong buổi phỏng vấn xin việc.")
                    .prompt("Read the self-introduction aloud professionally.")
                    .mode(SpeakingPromptMode.READ_ALOUD)
                    .referenceText("Hello, my name is Linh. I graduated with a degree in information technology "
                            + "and I have two years of experience building web applications. "
                            + "I am a careful team player and I am excited about this opportunity.")
                    .level("B1")
                    .category("Work")
                    .isPremium(false)
                    .isPublished(true)
                    .orderIndex(3)
                    .maxDurationSeconds(30)
                    .attemptLimit(10)
                    .build());

    private final SpeakingPromptRepository repository;

    /**
     * Inserts the READ_ALOUD seed prompts when none exist.
     *
     * @param args application startup arguments
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean alreadySeeded = repository.findAllByOrderByOrderIndexAsc().stream()
                .anyMatch(prompt -> prompt.getMode() == SpeakingPromptMode.READ_ALOUD);
        if (alreadySeeded) {
            log.debug("Skipping READ_ALOUD prompt seeding: scripted prompts already exist");
            return;
        }
        repository.saveAll(SEEDS);
        log.info("Seeded {} READ_ALOUD speaking prompts with reference text", SEEDS.size());
    }
}
