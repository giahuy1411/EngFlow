package com.datn.engflow.controller.speaking;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeakingEndpointCompatibilityTest {
    @Test
    void promptControllerPublishesSpeakingAndLegacyPromptEndpoints() throws Exception {
        Method list = SpeakingPromptController.class.getMethod("getAllPrompts", int.class, int.class);
        assertMappings(list.getAnnotation(GetMapping.class).value(),
                "/api/v1/speaking-prompts", "/api/v1/video-prompts");
    }

    @Test
    void submissionControllerPublishesSpeakingAndLegacyUploadEndpoints() {
        Method upload = Arrays.stream(SpeakingSubmissionController.class.getMethods())
                .filter(method -> method.getName().equals("uploadSubmission"))
                .findFirst()
                .orElseThrow();
        assertMappings(upload.getAnnotation(PostMapping.class).value(),
                "/api/v1/speaking-submissions/upload", "/api/v1/video-submissions/upload");
    }

    private static void assertMappings(String[] actual, String... expected) {
        for (String path : expected) {
            assertTrue(Arrays.asList(actual).contains(path), "Missing endpoint mapping: " + path);
        }
    }
}
