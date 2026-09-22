package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.BlockRequest;
import com.datn.engflow.model.dto.request.SectionRequest;
import com.datn.engflow.model.dto.response.BlockResponse;
import com.datn.engflow.model.dto.response.SectionResponse;
import com.datn.engflow.model.entity.LessonBlock;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.LessonSection;
import com.datn.engflow.model.enums.BlockType;
import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.repository.LessonBlockRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * class LessonStructureService.
 */
public class LessonStructureService {

    private final LessonSectionRepository sectionRepository;
    private final LessonBlockRepository blockRepository;
    private final LessonRepository lessonRepository;

    /**
     * audit-v7 F56: GET-mutation removed. Previously this auto-INSERTed a
     * section+block for every materialized lesson — on a PUBLIC endpoint
     * (GET /api/lessons/{id}/structure), that is an unauthenticated DB write
     * amplification (DoS). Reads are now pure: if nothing is materialized,
     * synthesize a read-only view from lesson.content without persisting.
     * Admin structure edits (add/update/delete) still materialize explicitly.
     */
    public List<SectionResponse> getLessonStructure(Long lessonId) {
        List<LessonSection> sections = sectionRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        if (!sections.isEmpty()) {
            return sections.stream().map(this::toSectionResponse).toList();
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        if (lesson.getContent() == null || lesson.getContent().isBlank()) {
            return List.of();
        }
        return List.of(virtualSection(lesson));
    }

    /** Transient (never persisted) section view used when a lesson isn't materialized. */
    private SectionResponse virtualSection(Lesson lesson) {
        return SectionResponse.builder()
                .id(null)
                .title("Nội dung bài học")
                .orderIndex(10)
                .blocks(List.of(BlockResponse.builder()
                        .id(null)
                        .blockType(com.datn.engflow.model.enums.BlockType.TEXT.name())
                        .data(lesson.getContent())
                        .orderIndex(10)
                        .build()))
                .build();
    }

    @Transactional
    public SectionResponse addSection(Long lessonId, SectionRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        LessonSection section = LessonSection.builder()
                .lesson(lesson)
                .title(request.getTitle())
                .orderIndex(request.getOrderIndex())
                .build();
        section = sectionRepository.save(section);
        return toSectionResponse(section);
    }

    @Transactional
    public SectionResponse updateSection(Long sectionId, SectionRequest request) {
        LessonSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("LessonSection", "id", sectionId));
        section.setTitle(request.getTitle());
        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }
        section = sectionRepository.save(section);
        return toSectionResponse(section);
    }

    @Transactional
    public void deleteSection(Long sectionId) {
        blockRepository.deleteBySectionId(sectionId);
        sectionRepository.deleteById(sectionId);
    }

    /**
     * audit-v13 F-13-02: block types that have NO learner renderer and NO grading path.
     * The admin UI already hides them; the API must not accept them either, or the
     * "cannot be created any more" claim would be UI-only. Existing blocks of these types
     * are still readable/updatable so nothing on disk is orphaned.
     */
    private static final java.util.Set<BlockType> UNSUPPORTED_BLOCK_TYPES =
            java.util.EnumSet.of(BlockType.QUESTION, BlockType.SUBMISSION);

    private BlockType parseBlockType(String raw) {
        BlockType type;
        try {
            type = BlockType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Loại block không hợp lệ: " + raw);
        }
        if (UNSUPPORTED_BLOCK_TYPES.contains(type)) {
            throw new BadRequestException(
                    "Loại block " + type + " chưa được hỗ trợ hiển thị cho học viên. "
                            + "Dùng mục Quản lý bài tập để tạo câu hỏi.");
        }
        return type;
    }

    @Transactional
    public SectionResponse addBlock(Long sectionId, BlockRequest request) {
        LessonSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("LessonSection", "id", sectionId));
        LessonBlock block = LessonBlock.builder()
                .section(section)
                .blockType(parseBlockType(request.getBlockType()))
                .data(request.getData())
                .orderIndex(request.getOrderIndex())
                .build();
        block = blockRepository.save(block);
        return toSectionResponse(section);
    }

    @Transactional
    public SectionResponse updateBlock(Long blockId, BlockRequest request) {
        LessonBlock block = blockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("LessonBlock", "id", blockId));
        if (request.getBlockType() != null) {
            // Changing an EXISTING block to an unsupported type is refused; a block already
            // of that type may still be edited (its type is unchanged, so this is a no-op).
            if (!block.getBlockType().name().equals(request.getBlockType())) {
                block.setBlockType(parseBlockType(request.getBlockType()));
            }
        }
        if (request.getData() != null) {
            block.setData(request.getData());
        }
        if (request.getOrderIndex() != null) {
            block.setOrderIndex(request.getOrderIndex());
        }
        blockRepository.save(block);
        return toSectionResponse(block.getSection());
    }

    @Transactional
    public void deleteBlock(Long blockId) {
        blockRepository.deleteById(blockId);
    }

    private SectionResponse toSectionResponse(LessonSection section) {
        List<LessonBlock> blocks = blockRepository.findBySectionIdOrderByOrderIndexAsc(section.getId());
        return SectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .orderIndex(section.getOrderIndex())
                .blocks(blocks.stream().map(b -> BlockResponse.builder()
                        .id(b.getId())
                        .blockType(b.getBlockType().name())
                        .data(b.getData())
                        .orderIndex(b.getOrderIndex())
                        .build()).toList())
                .build();
    }
}
