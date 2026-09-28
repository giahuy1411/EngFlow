package com.datn.engflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Publishes the shared Jackson {@link ObjectMapper} used to read and write JSON
 * across the application (AI request bodies, LLM responses, crawler seed files).
 *
 * <p>The bean is marked {@code @Primary} so it wins over the mapper that Spring
 * Boot's Jackson auto-configuration contributes, keeping a single serialization
 * policy for the whole context. Several services inject it by constructor —
 * {@code AiExerciseService}, {@code JsonDataSeeder} and
 * {@link com.datn.engflow.service.assessment.SpeakingAssessmentService} — while a
 * few older code paths build their own throwaway {@code new ObjectMapper()}
 * instead; those deliberately bypass this configuration.</p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Exposes a default-configured {@link ObjectMapper} as the primary bean.
     *
     * <p>No features are registered here: parsing uses the library defaults, so
     * unknown JSON properties still fail unless a caller configures its own
     * mapper.</p>
     *
     * @return the primary mapper instance shared by all Jackson consumers
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
