package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoLessonServiceTest {

    @Test
    void extractsVideoIdFromCommonUrlShapes() {
        assertThat(VideoLessonService.extractVideoId("https://www.youtube.com/watch?v=Ba4NAs-ACPY"))
                .isEqualTo("Ba4NAs-ACPY");
        assertThat(VideoLessonService.extractVideoId("https://youtu.be/Ba4NAs-ACPY?t=12"))
                .isEqualTo("Ba4NAs-ACPY");
        assertThat(VideoLessonService.extractVideoId("https://www.youtube.com/embed/Ba4NAs-ACPY"))
                .isEqualTo("Ba4NAs-ACPY");
        assertThat(VideoLessonService.extractVideoId("https://m.youtube.com/shorts/Ba4NAs-ACPY"))
                .isEqualTo("Ba4NAs-ACPY");
        assertThat(VideoLessonService.extractVideoId("Ba4NAs-ACPY")).isEqualTo("Ba4NAs-ACPY");
    }

    @Test
    void rejectsUnrecognizedUrl() {
        assertThatThrownBy(() -> VideoLessonService.extractVideoId("https://vimeo.com/12345"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> VideoLessonService.extractVideoId(null))
                .isInstanceOf(BadRequestException.class);
    }
}
