package com.datn.engflow.service;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the TTS branch of {@link AiExerciseService#generateListening}:
 * audio is synthesized and uploaded only when the TTS service is available,
 * and any per-exercise failure must not abort the batch.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiExerciseServiceListeningTtsTest {

    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private TtsService ttsService;
    @Mock
    private CloudinaryService cloudinaryService;

    private AiExerciseService service;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        service = spy(new AiExerciseService(
                new ObjectMapper(),
                null, // research service unused in the stubbed path below
                ttsService,
                cloudinaryService,
                exerciseRepository,
                lessonRepository));
        lesson = new Lesson();
        ReflectionTestUtils.setField(lesson, "id", 567L);
        ReflectionTestUtils.setField(lesson, "title", "Present Simple");
    }

    private Exercise listeningExercise(String question) {
        Exercise ex = new Exercise();
        ReflectionTestUtils.setField(ex, "question", question);
        return ex;
    }

    private void stubGeneration(Exercise... exercises) {
        // Bypass Ollama: the TTS branch is what this test covers.
        doReturnGeneration(List.of(exercises));
    }

    private void doReturnGeneration(List<Exercise> exercises) {
        ReflectionTestUtils.setField(service, "ollamaBaseUrl", "http://localhost:0");
        org.mockito.Mockito.doReturn(exercises).when(service)
                .generateByType(any(Lesson.class), any(), anyInt());
    }

    @Test
    @DisplayName("generateListening: TTS unavailable -> exercises saved WITHOUT audio")
    void generateListening_ttsUnavailable_savesExerciseWithoutAudio() throws Exception {
        Exercise ex = listeningExercise("What do you do every morning?");
        stubGeneration(ex);
        when(ttsService.isAvailable()).thenReturn(false);

        List<Exercise> result = service.generateListening(lesson, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAudioUrl()).isNull();
        verify(ttsService, never()).synthesize(anyString(), any(), anyString());
        verify(cloudinaryService, never()).uploadAudioBytes(any(), anyString());
    }

    @Test
    @DisplayName("generateListening: TTS returns bytes -> uploaded to Cloudinary, audioUrl set")
    void generateListening_ttsReturnsBytes_uploadsToCloudinary() throws Exception {
        Exercise ex = listeningExercise("She works on Sundays.");
        stubGeneration(ex);
        when(ttsService.isAvailable()).thenReturn(true);
        when(ttsService.synthesize(eq("She works on Sundays."), any(), eq("en")))
                .thenReturn(new byte[]{1, 2, 3, 4});
        when(cloudinaryService.uploadAudioBytes(any(), anyString()))
                .thenReturn("https://cdn.example/audio.wav");

        List<Exercise> result = service.generateListening(lesson, 1);

        assertThat(result.get(0).getAudioUrl()).isEqualTo("https://cdn.example/audio.wav");
        verify(cloudinaryService).uploadAudioBytes(any(), anyString());
    }

    @Test
    @DisplayName("generateListening: TTS throws -> batch continues, other exercises still processed")
    void generateListening_ttsThrows_logsAndContinues() throws Exception {
        Exercise bad = listeningExercise("First question?");
        Exercise good = listeningExercise("Second question?");
        stubGeneration(bad, good);
        when(ttsService.isAvailable()).thenReturn(true);
        when(ttsService.synthesize(eq("First question?"), any(), eq("en")))
                .thenThrow(new RuntimeException("boom"));
        when(ttsService.synthesize(eq("Second question?"), any(), eq("en")))
                .thenReturn(new byte[]{9});
        when(cloudinaryService.uploadAudioBytes(any(), anyString()))
                .thenReturn("https://cdn.example/ok.wav");

        List<Exercise> result = service.generateListening(lesson, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAudioUrl()).isNull();
        assertThat(result.get(1).getAudioUrl()).isEqualTo("https://cdn.example/ok.wav");
    }

    @Test
    @DisplayName("generateListening: TTS returns null -> no upload, no audioUrl")
    void generateListening_ttsReturnsNull_skipsUpload() throws Exception {
        Exercise ex = listeningExercise("Any answer?");
        stubGeneration(ex);
        when(ttsService.isAvailable()).thenReturn(true);
        when(ttsService.synthesize(anyString(), any(), anyString())).thenReturn(null);

        List<Exercise> result = service.generateListening(lesson, 1);

        assertThat(result.get(0).getAudioUrl()).isNull();
        verify(cloudinaryService, never()).uploadAudioBytes(any(), anyString());
    }
}
