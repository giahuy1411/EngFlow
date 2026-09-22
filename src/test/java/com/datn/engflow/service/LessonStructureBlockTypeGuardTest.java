package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.model.dto.request.BlockRequest;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.LessonBlock;
import com.datn.engflow.model.entity.LessonSection;
import com.datn.engflow.model.enums.BlockType;
import com.datn.engflow.repository.LessonBlockRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regression test for audit-v13 F-13-02 (server-side half).
 *
 * <p>The admin UI hides QUESTION/SUBMISSION block types, but the API still accepted them —
 * so "cannot be created any more" was a UI-only claim. These tests pin the server guard,
 * and confirm it does not over-block the supported types.
 *
 * <p>Existing blocks of an unsupported type must remain editable (nothing on disk is orphaned).
 */
@ExtendWith(MockitoExtension.class)
class LessonStructureBlockTypeGuardTest {

    @Mock private LessonSectionRepository sectionRepository;
    @Mock private LessonBlockRepository blockRepository;
    @Mock private LessonRepository lessonRepository;

    private LessonStructureService service;

    @BeforeEach
    void setUp() {
        service = new LessonStructureService(sectionRepository, blockRepository, lessonRepository);
    }

    private BlockRequest blockRequest(String blockType) {
        BlockRequest r = new BlockRequest();
        r.setBlockType(blockType);
        r.setData("{}");
        return r;
    }

    private void sectionExists() {
        Lesson lesson = Lesson.builder().id(1L).title("L").build();
        when(sectionRepository.findById(5L))
                .thenReturn(Optional.of(LessonSection.builder().id(5L).lesson(lesson).title("S").build()));
    }

    // --- addBlock: unsupported types refused ---

    @Test
    void addBlockRejectsQuestionType() {
        sectionExists();
        assertThatThrownBy(() -> service.addBlock(5L, blockRequest("QUESTION")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("QUESTION");
    }

    @Test
    void addBlockRejectsSubmissionType() {
        sectionExists();
        assertThatThrownBy(() -> service.addBlock(5L, blockRequest("SUBMISSION")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SUBMISSION");
    }

    @Test
    void addBlockRejectsUnknownType() {
        sectionExists();
        assertThatThrownBy(() -> service.addBlock(5L, blockRequest("NOT_A_TYPE")))
                .isInstanceOf(BadRequestException.class);
    }

    // --- addBlock: supported types still accepted (no over-blocking) ---

    @Test
    void addBlockAcceptsSupportedTypes() {
        sectionExists();
        when(blockRepository.save(any(LessonBlock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(blockRepository.findBySectionIdOrderByOrderIndexAsc(5L)).thenReturn(java.util.List.of());

        for (String type : new String[]{"TEXT", "IMAGE", "AUDIO", "TABLE"}) {
            assertThatCode(() -> service.addBlock(5L, blockRequest(type)))
                    .as("type %s must stay allowed", type)
                    .doesNotThrowAnyException();
        }
    }

    // --- updateBlock: may not switch TO an unsupported type, but an existing one stays editable ---

    @Test
    void updateBlockRejectsSwitchingToQuestion() {
        Lesson lesson = Lesson.builder().id(1L).build();
        LessonSection section = LessonSection.builder().id(5L).lesson(lesson).title("S").build();
        LessonBlock existing = LessonBlock.builder().id(9L).section(section)
                .blockType(BlockType.TEXT).data("{}").build();
        when(blockRepository.findById(9L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateBlock(9L, blockRequest("QUESTION")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateBlockAllowsEditingAnExistingQuestionBlock() {
        Lesson lesson = Lesson.builder().id(1L).build();
        LessonSection section = LessonSection.builder().id(5L).lesson(lesson).title("S").build();
        // Real DB has 3 QUESTION blocks (measured 2026-09-22) — they must stay editable.
        LessonBlock existing = LessonBlock.builder().id(4L).section(section)
                .blockType(BlockType.QUESTION).data("{}").build();
        when(blockRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(blockRepository.save(any(LessonBlock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(blockRepository.findBySectionIdOrderByOrderIndexAsc(5L)).thenReturn(java.util.List.of());

        BlockRequest req = new BlockRequest();
        req.setBlockType("QUESTION");            // same type -> no-op, must not throw
        req.setData("{\"questionText\":\"edited\"}");
        assertThatCode(() -> service.updateBlock(4L, req)).doesNotThrowAnyException();
    }
}
