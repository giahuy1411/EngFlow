package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.SnapshotResponse;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.LessonBlock;
import com.datn.engflow.model.entity.LessonSection;
import com.datn.engflow.model.entity.LessonSnapshot;
import com.datn.engflow.model.enums.BlockType;
import com.datn.engflow.repository.LessonBlockRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSectionRepository;
import com.datn.engflow.repository.LessonSnapshotRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.datn.engflow.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * class LessonSnapshotService.
 */
public class LessonSnapshotService {

    private final LessonSnapshotRepository snapshotRepository;
    private final LessonRepository lessonRepository;
    private final LessonSectionRepository sectionRepository;
    private final LessonBlockRepository blockRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void takeSnapshot(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        List<LessonSection> sections = sectionRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        // LinkedHashMap (không phải Map.of) vì section title / block data có thể null
        // với lesson dở build — snapshot không được phép 500 vì field tùy chọn.
        List<Map<String, Object>> snapshotData = sections.stream().map(s -> {
            List<LessonBlock> blocks = blockRepository.findBySectionIdOrderByOrderIndexAsc(s.getId());
            Map<String, Object> sectionMap = new LinkedHashMap<>();
            sectionMap.put("title", s.getTitle());
            sectionMap.put("orderIndex", s.getOrderIndex());
            sectionMap.put("blocks", blocks.stream().map(b -> {
                Map<String, Object> blockMap = new LinkedHashMap<>();
                blockMap.put("blockType", b.getBlockType() != null ? b.getBlockType().name() : null);
                blockMap.put("data", b.getData());
                blockMap.put("orderIndex", b.getOrderIndex());
                return blockMap;
            }).toList());
            return sectionMap;
        }).toList();

        try {
            String json = objectMapper.writeValueAsString(snapshotData);
            LessonSnapshot snapshot = LessonSnapshot.builder()
                    .lesson(lesson)
                    .snapshot(json)
                    .createdBy(userId)
                    .build();
            snapshotRepository.save(snapshot);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize snapshot", e);
        }
    }

    @Transactional(readOnly = true)
    public List<SnapshotResponse> getSnapshots(Long lessonId) {
        return snapshotRepository.findByLessonIdOrderByCreatedAtDesc(lessonId)
                .stream()
                .map(s -> SnapshotResponse.builder()
                        .id(s.getId())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public List<Map<String, Object>> restoreSnapshot(Long lessonId, Long snapshotId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        LessonSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot", "id", snapshotId));

        List<Map<String, Object>> sectionsData;
        try {
            sectionsData = objectMapper.readValue(snapshot.getSnapshot(),
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse snapshot", e);
        }

        List<LessonSection> existing = sectionRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        for (LessonSection s : existing) {
            blockRepository.deleteBySectionId(s.getId());
            sectionRepository.delete(s);
        }

        for (Map<String, Object> sectionMap : sectionsData) {
            LessonSection section = LessonSection.builder()
                    .lesson(lesson)
                    .title((String) sectionMap.get("title"))
                    .orderIndex((Integer) sectionMap.get("orderIndex"))
                    .build();
            section = sectionRepository.save(section);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blocksData = (List<Map<String, Object>>) sectionMap.get("blocks");
            if (blocksData != null) {
                for (Map<String, Object> blockMap : blocksData) {
                    LessonBlock block = LessonBlock.builder()
                            .section(section)
                            .blockType(BlockType.valueOf((String) blockMap.get("blockType")))
                            .data((String) blockMap.get("data"))
                            .orderIndex((Integer) blockMap.get("orderIndex"))
                            .build();
                    blockRepository.save(block);
                }
            }
        }

        List<LessonSection> restored = sectionRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        return restored.stream().map(s -> {
            List<LessonBlock> blocks = blockRepository.findBySectionIdOrderByOrderIndexAsc(s.getId());
            Map<String, Object> sectionMap = new LinkedHashMap<>();
            sectionMap.put("id", s.getId());
            sectionMap.put("title", s.getTitle());
            sectionMap.put("orderIndex", s.getOrderIndex());
            sectionMap.put("blocks", blocks.stream().map(b -> {
                Map<String, Object> blockMap = new LinkedHashMap<>();
                blockMap.put("id", b.getId());
                blockMap.put("blockType", b.getBlockType() != null ? b.getBlockType().name() : null);
                blockMap.put("data", b.getData());
                blockMap.put("orderIndex", b.getOrderIndex());
                return blockMap;
            }).toList());
            return sectionMap;
        }).toList();
    }
}
