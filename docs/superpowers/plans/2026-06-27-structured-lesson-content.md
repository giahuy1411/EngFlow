# Structured Lesson Content - Test/Prototype

> **For agentic workers:** Subagent-driven development recommended.

**Goal:** Build a test/prototype where admin can create lessons using structured blocks (text, image, audio, table, question, submission) instead of raw HTML content.

**Architecture:** New entities `LessonSection` + `LessonBlock` store structured content. Admin builder UI in Vue 3. Student renderer displays blocks. Existing content untouched.

**Tech Stack:** Spring Boot 4.0.6 (backend), Vue 3 + Vite (frontend), Tailwind CSS

---

### Task 1: Backend Entities & Enums

**Files:**
- Create: `src/main/java/com/datn/engflow/model/enums/BlockType.java`
- Create: `src/main/java/com/datn/engflow/model/enums/QuestionType.java`
- Create: `src/main/java/com/datn/engflow/model/entity/LessonSection.java`
- Create: `src/main/java/com/datn/engflow/model/entity/LessonBlock.java`
- Create: `src/main/java/com/datn/engflow/repository/LessonSectionRepository.java`
- Create: `src/main/java/com/datn/engflow/repository/LessonBlockRepository.java`

- [ ] **Step 1: Create BlockType enum**

```java
package com.datn.engflow.model.enums;

public enum BlockType {
    TEXT,
    IMAGE,
    AUDIO,
    TABLE,
    QUESTION,
    SUBMISSION
}
```

- [ ] **Step 2: Create QuestionType enum**

```java
package com.datn.engflow.model.enums;

public enum QuestionType {
    MULTIPLE_CHOICE,
    FILL_IN_BLANK,
    TRUE_FALSE,
    MATCHING
}
```

- [ ] **Step 3: Create LessonSection entity**

```java
package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @ToString.Exclude
    private Lesson lesson;

    @Column(nullable = false)
    private String title;

    @Column(name = "order_index")
    private Integer orderIndex;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create LessonBlock entity**

```java
package com.datn.engflow.model.entity;

import com.datn.engflow.model.enums.BlockType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    @ToString.Exclude
    private LessonSection section;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false)
    private BlockType blockType;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String data;

    @Column(name = "order_index")
    private Integer orderIndex;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 5: Create LessonSectionRepository**

```java
package com.datn.engflow.repository;

import com.datn.engflow.model.entity.LessonSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonSectionRepository extends JpaRepository<LessonSection, Long> {
    List<LessonSection> findByLessonIdOrderByOrderIndexAsc(Long lessonId);
    void deleteByLessonId(Long lessonId);
}
```

- [ ] **Step 6: Create LessonBlockRepository**

```java
package com.datn.engflow.repository;

import com.datn.engflow.model.entity.LessonBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonBlockRepository extends JpaRepository<LessonBlock, Long> {
    List<LessonBlock> findBySectionIdOrderByOrderIndexAsc(Long sectionId);
    void deleteBySectionId(Long sectionId);
}
```

---

### Task 2: Backend DTOs

**Files:**
- Create: `src/main/java/com/datn/engflow/model/dto/request/SectionRequest.java`
- Create: `src/main/java/com/datn/engflow/model/dto/request/BlockRequest.java`
- Create: `src/main/java/com/datn/engflow/model/dto/response/SectionResponse.java`
- Create: `src/main/java/com/datn/engflow/model/dto/response/BlockResponse.java`

- [ ] **Step 1: Create SectionRequest**

```java
package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SectionRequest {
    @NotBlank
    private String title;
    private Integer orderIndex;
}
```

- [ ] **Step 2: Create BlockRequest**

```java
package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlockRequest {
    @NotBlank
    private String blockType;
    private String data;
    private Integer orderIndex;
}
```

- [ ] **Step 3: Create SectionResponse**

```java
package com.datn.engflow.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class SectionResponse {
    private Long id;
    private String title;
    private Integer orderIndex;
    private List<BlockResponse> blocks;
}
```

- [ ] **Step 4: Create BlockResponse**

```java
package com.datn.engflow.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BlockResponse {
    private Long id;
    private String blockType;
    private String data;
    private Integer orderIndex;
}
```

---

### Task 3: Backend Service

**Files:**
- Create: `src/main/java/com/datn/engflow/service/LessonStructureService.java`
- Create: `src/main/java/com/datn/engflow/service/LessonStructureServiceImpl.java`

- [ ] **Step 1: Create LessonStructureService interface**

```java
package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.BlockRequest;
import com.datn.engflow.model.dto.request.SectionRequest;
import com.datn.engflow.model.dto.response.SectionResponse;

import java.util.List;

public interface LessonStructureService {
    List<SectionResponse> getLessonStructure(Long lessonId);
    SectionResponse addSection(Long lessonId, SectionRequest request);
    SectionResponse updateSection(Long sectionId, SectionRequest request);
    void deleteSection(Long sectionId);
    SectionResponse addBlock(Long sectionId, BlockRequest request);
    SectionResponse updateBlock(Long blockId, BlockRequest request);
    void deleteBlock(Long blockId);
}
```

- [ ] **Step 2: Create LessonStructureServiceImpl**

```java
package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.BlockRequest;
import com.datn.engflow.model.dto.request.SectionRequest;
import com.datn.engflow.model.dto.response.BlockResponse;
import com.datn.engflow.model.dto.response.SectionResponse;
import com.datn.engflow.model.entity.LessonBlock;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.LessonSection;
import com.datn.engflow.model.enums.BlockType;
import com.datn.engflow.repository.LessonBlockRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonStructureServiceImpl implements LessonStructureService {

    private final LessonSectionRepository sectionRepository;
    private final LessonBlockRepository blockRepository;
    private final LessonRepository lessonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getLessonStructure(Long lessonId) {
        List<LessonSection> sections = sectionRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        return sections.stream().map(this::toSectionResponse).toList();
    }

    @Override
    @Transactional
    public SectionResponse addSection(Long lessonId, SectionRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found: " + lessonId));
        LessonSection section = LessonSection.builder()
                .lesson(lesson)
                .title(request.getTitle())
                .orderIndex(request.getOrderIndex())
                .build();
        section = sectionRepository.save(section);
        return toSectionResponse(section);
    }

    @Override
    @Transactional
    public SectionResponse updateSection(Long sectionId, SectionRequest request) {
        LessonSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found: " + sectionId));
        section.setTitle(request.getTitle());
        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }
        section = sectionRepository.save(section);
        return toSectionResponse(section);
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId) {
        blockRepository.deleteBySectionId(sectionId);
        sectionRepository.deleteById(sectionId);
    }

    @Override
    @Transactional
    public SectionResponse addBlock(Long sectionId, BlockRequest request) {
        LessonSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found: " + sectionId));
        LessonBlock block = LessonBlock.builder()
                .section(section)
                .blockType(BlockType.valueOf(request.getBlockType()))
                .data(request.getData())
                .orderIndex(request.getOrderIndex())
                .build();
        block = blockRepository.save(block);
        return toSectionResponse(section);
    }

    @Override
    @Transactional
    public SectionResponse updateBlock(Long blockId, BlockRequest request) {
        LessonBlock block = blockRepository.findById(blockId)
                .orElseThrow(() -> new RuntimeException("Block not found: " + blockId));
        if (request.getBlockType() != null) {
            block.setBlockType(BlockType.valueOf(request.getBlockType()));
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

    @Override
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
```

---

### Task 4: Backend Controller

**Files:**
- Create: `src/main/java/com/datn/engflow/controller/LessonStructureController.java`

- [ ] **Step 1: Create LessonStructureController**

```java
package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.BlockRequest;
import com.datn.engflow.model.dto.request.SectionRequest;
import com.datn.engflow.model.dto.response.SectionResponse;
import com.datn.engflow.service.LessonStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LessonStructureController {

    private final LessonStructureService lessonStructureService;

    // Admin endpoints
    @GetMapping("/api/admin/lessons/{lessonId}/structure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SectionResponse>> getStructure(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonStructureService.getLessonStructure(lessonId));
    }

    @PostMapping("/api/admin/lessons/{lessonId}/sections")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SectionResponse> addSection(@PathVariable Long lessonId, @RequestBody SectionRequest request) {
        return ResponseEntity.ok(lessonStructureService.addSection(lessonId, request));
    }

    @PutMapping("/api/admin/sections/{sectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SectionResponse> updateSection(@PathVariable Long sectionId, @RequestBody SectionRequest request) {
        return ResponseEntity.ok(lessonStructureService.updateSection(sectionId, request));
    }

    @DeleteMapping("/api/admin/sections/{sectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        lessonStructureService.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/sections/{sectionId}/blocks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SectionResponse> addBlock(@PathVariable Long sectionId, @RequestBody BlockRequest request) {
        return ResponseEntity.ok(lessonStructureService.addBlock(sectionId, request));
    }

    @PutMapping("/api/admin/blocks/{blockId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SectionResponse> updateBlock(@PathVariable Long blockId, @RequestBody BlockRequest request) {
        return ResponseEntity.ok(lessonStructureService.updateBlock(blockId, request));
    }

    @DeleteMapping("/api/admin/blocks/{blockId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBlock(@PathVariable Long blockId) {
        lessonStructureService.deleteBlock(blockId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(filename);
            Files.write(filePath, file.getBytes());
            String url = "/api/resources/" + filename;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // User-facing endpoint
    @GetMapping("/api/lessons/{lessonId}/structure")
    public ResponseEntity<List<SectionResponse>> getLessonStructure(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonStructureService.getLessonStructure(lessonId));
    }
}
```

---

### Task 5: Frontend API Service

**Files:**
- Create: `frontend/src/services/lessonStructureService.js`

- [ ] **Step 1: Create lessonStructureService.js**

```javascript
import api from './api';

export default {
  getStructure(lessonId) {
    return api.get(`/api/lessons/${lessonId}/structure`).then(r => r.data);
  },
  getAdminStructure(lessonId) {
    return api.get(`/api/admin/lessons/${lessonId}/structure`).then(r => r.data);
  },
  addSection(lessonId, data) {
    return api.post(`/api/admin/lessons/${lessonId}/sections`, data).then(r => r.data);
  },
  updateSection(sectionId, data) {
    return api.put(`/api/admin/sections/${sectionId}`, data).then(r => r.data);
  },
  deleteSection(sectionId) {
    return api.delete(`/api/admin/sections/${sectionId}`);
  },
  addBlock(sectionId, data) {
    return api.post(`/api/admin/sections/${sectionId}/blocks`, data).then(r => r.data);
  },
  updateBlock(blockId, data) {
    return api.put(`/api/admin/blocks/${blockId}`, data).then(r => r.data);
  },
  deleteBlock(blockId) {
    return api.delete(`/api/admin/blocks/${blockId}`);
  },
  uploadFile(file) {
    const form = new FormData();
    form.append('file', file);
    return api.post('/api/admin/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(r => r.data);
  }
};
```

---

### Task 6: Frontend Block Renderer Components

**Files:**
- Create: `frontend/src/components/lessons/block-renderer/TextBlock.vue`
- Create: `frontend/src/components/lessons/block-renderer/ImageBlock.vue`
- Create: `frontend/src/components/lessons/block-renderer/AudioBlock.vue`
- Create: `frontend/src/components/lessons/block-renderer/TableBlock.vue`
- Create: `frontend/src/components/lessons/block-renderer/QuestionBlock.vue`
- Create: `frontend/src/components/lessons/block-renderer/SubmissionBlock.vue`
- Create: `frontend/src/components/lessons/block-renderer/LessonBlockRenderer.vue`

- [ ] **Step 1: Create TextBlock.vue**

```vue
<template>
  <div class="prose max-w-none" v-html="data.content"></div>
</template>

<script setup>
const props = defineProps({
  data: { type: Object, required: true }
});
</script>
```

- [ ] **Step 2: Create ImageBlock.vue**

```vue
<template>
  <div class="my-4">
    <img :src="data.imageUrl" :alt="data.caption || ''" class="max-w-full rounded-lg" />
    <p v-if="data.caption" class="text-sm text-gray-500 mt-2 italic">{{ data.caption }}</p>
  </div>
</template>

<script setup>
const props = defineProps({
  data: { type: Object, required: true }
});
</script>
```

- [ ] **Step 3: Create AudioBlock.vue**

```vue
<template>
  <div class="my-4 p-4 bg-gray-50 rounded-lg">
    <audio controls class="w-full" :src="data.audioUrl"></audio>
    <p v-if="data.transcript" class="text-sm text-gray-500 mt-2">{{ data.transcript }}</p>
  </div>
</template>

<script setup>
const props = defineProps({
  data: { type: Object, required: true }
});
</script>
```

- [ ] **Step 4: Create TableBlock.vue**

```vue
<template>
  <div class="my-4 overflow-x-auto">
    <table class="min-w-full border-collapse border">
      <thead v-if="data.headers?.length">
        <tr>
          <th v-for="h in data.headers" :key="h" class="border px-4 py-2 bg-gray-100">{{ h }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, i) in data.rows" :key="i">
          <td v-for="(cell, j) in row" :key="j" class="border px-4 py-2">{{ cell }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
const props = defineProps({
  data: { type: Object, required: true }
});
</script>
```

- [ ] **Step 5: Create QuestionBlock.vue**

```vue
<template>
  <div class="my-4 p-4 border rounded-lg">
    <p class="font-medium mb-3">{{ data.questionText }}</p>
    <img v-if="data.imageUrl" :src="data.imageUrl" class="max-w-full rounded mb-3" />
    <div v-if="data.questionType === 'MULTIPLE_CHOICE'" class="space-y-2">
      <label v-for="(opt, i) in parsedOptions" :key="i"
        class="flex items-center gap-2 p-2 rounded cursor-pointer"
        :class="selectedAnswer === opt ? 'bg-blue-100 border border-blue-300' : 'bg-gray-50 hover:bg-gray-100'">
        <input type="radio" :name="'q-' + blockId" :value="opt" @change="selectedAnswer = opt" />
        {{ opt }}
      </label>
    </div>
    <div v-else-if="data.questionType === 'FILL_IN_BLANK'" class="mt-2">
      <input v-model="fillAnswer" type="text" class="border rounded px-3 py-2 w-full"
        placeholder="Nhập câu trả lời..." />
    </div>
    <div v-else-if="data.questionType === 'TRUE_FALSE'" class="flex gap-4 mt-2">
      <button @click="tfAnswer = 'True'"
        class="px-6 py-2 rounded" :class="tfAnswer === 'True' ? 'bg-green-500 text-white' : 'bg-gray-100'">True</button>
      <button @click="tfAnswer = 'False'"
        class="px-6 py-2 rounded" :class="tfAnswer === 'False' ? 'bg-red-500 text-white' : 'bg-gray-100'">False</button>
    </div>
    <div v-else-if="data.questionType === 'MATCHING'" class="mt-2">
      <p class="text-gray-500 italic">Tính năng matching đang phát triển...</p>
    </div>
    <button v-if="showCheck" @click="checkAnswer"
      class="mt-3 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
      Kiểm tra
    </button>
    <div v-if="result !== null" class="mt-2" :class="result ? 'text-green-600' : 'text-red-600'">
      {{ result ? 'Chính xác!' : 'Sai rồi!' }}
      <span v-if="data.explanation" class="block text-gray-600 text-sm mt-1">{{ data.explanation }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  data: { type: Object, required: true },
  blockId: { type: Number }
});

const selectedAnswer = ref(null);
const fillAnswer = ref('');
const tfAnswer = ref(null);
const result = ref(null);

const parsedOptions = computed(() => {
  if (typeof props.data.options === 'string') {
    try { return JSON.parse(props.data.options); } catch { return []; }
  }
  return props.data.options || [];
});

const showCheck = computed(() => {
  if (props.data.questionType === 'MULTIPLE_CHOICE') return selectedAnswer.value !== null;
  if (props.data.questionType === 'FILL_IN_BLANK') return fillAnswer.value.trim() !== '';
  if (props.data.questionType === 'TRUE_FALSE') return tfAnswer.value !== null;
  return false;
});

function checkAnswer() {
  let correct = false;
  if (props.data.questionType === 'MULTIPLE_CHOICE') {
    correct = selectedAnswer.value?.trim().toLowerCase() === props.data.correctAnswer?.trim().toLowerCase();
  } else if (props.data.questionType === 'FILL_IN_BLANK') {
    correct = fillAnswer.value.trim().toLowerCase() === props.data.correctAnswer?.trim().toLowerCase();
  } else if (props.data.questionType === 'TRUE_FALSE') {
    correct = tfAnswer.value?.toLowerCase() === props.data.correctAnswer?.trim().toLowerCase();
  }
  result.value = correct;
}
</script>
```

- [ ] **Step 6: Create SubmissionBlock.vue**

```vue
<template>
  <div class="my-4 p-4 border rounded-lg bg-gray-50">
    <p class="font-medium mb-3">{{ data.prompt }}</p>
    <textarea v-if="data.submissionType === 'TEXT'" v-model="text"
      class="w-full border rounded px-3 py-2 min-h-[120px]" placeholder="Nhập câu trả lời..."></textarea>
    <div v-else-if="data.submissionType === 'AUDIO'" class="text-gray-500 italic">
      Tính năng ghi âm đang phát triển...
    </div>
    <p v-if="submitted" class="text-green-600 mt-2">Đã nộp bài!</p>
  </div>
</template>

<script setup>
import { ref } from 'vue';
const props = defineProps({
  data: { type: Object, required: true }
});
const text = ref('');
const submitted = ref(false);
// TODO: Connect to submission API
</script>
```

- [ ] **Step 7: Create LessonBlockRenderer.vue**

```vue
<template>
  <div>
    <div v-for="section in sections" :key="section.id" class="mb-8">
      <h2 class="text-xl font-bold mb-4">{{ section.title }}</h2>
      <div v-for="block in section.blocks" :key="block.id" class="mb-4">
        <TextBlock v-if="block.blockType === 'TEXT'" :data="parseData(block.data)" />
        <ImageBlock v-else-if="block.blockType === 'IMAGE'" :data="parseData(block.data)" />
        <AudioBlock v-else-if="block.blockType === 'AUDIO'" :data="parseData(block.data)" />
        <TableBlock v-else-if="block.blockType === 'TABLE'" :data="parseData(block.data)" />
        <QuestionBlock v-else-if="block.blockType === 'QUESTION'" :data="parseData(block.data)" :blockId="block.id" />
        <SubmissionBlock v-else-if="block.blockType === 'SUBMISSION'" :data="parseData(block.data)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import TextBlock from './TextBlock.vue';
import ImageBlock from './ImageBlock.vue';
import AudioBlock from './AudioBlock.vue';
import TableBlock from './TableBlock.vue';
import QuestionBlock from './QuestionBlock.vue';
import SubmissionBlock from './SubmissionBlock.vue';

const props = defineProps({
  sections: { type: Array, required: true }
});

function parseData(data) {
  if (!data) return {};
  if (typeof data === 'string') {
    try { return JSON.parse(data); } catch { return { content: data }; }
  }
  return data;
}
</script>
```

---

### Task 7: Frontend Admin Lesson Builder

**Files:**
- Create: `frontend/src/views/admin/AdminLessonBuilder.vue`

- [ ] **Step 1: Create AdminLessonBuilder.vue**

This is the main admin page for building structured lessons. It includes:
- Section management (add/edit/delete sections)
- Block management (add/edit/delete blocks per section)
- File upload for images and audio
- JSON preview of block data

Since this file is large, here's the complete implementation:

```vue
<template>
  <div class="p-6 max-w-4xl mx-auto">
    <h1 class="text-2xl font-bold mb-6">Xây dựng bài học: {{ lesson?.title }}</h1>

    <div v-if="loading" class="text-gray-500">Đang tải...</div>

    <template v-else>
      <!-- Sections -->
      <div v-for="(section, si) in sections" :key="section.id" class="mb-6 border rounded-lg p-4">
        <div class="flex items-center justify-between mb-3">
          <input v-model="section.title" @change="updateSection(section)"
            class="text-lg font-semibold border-b border-transparent focus:border-blue-500 outline-none bg-transparent" />
          <div class="flex gap-2">
            <button @click="addBlock(section.id)" class="text-sm px-3 py-1 bg-green-100 text-green-700 rounded">+ Block</button>
            <button @click="deleteSection(section.id, si)" class="text-sm px-3 py-1 bg-red-100 text-red-700 rounded">Xóa</button>
          </div>
        </div>

        <!-- Blocks -->
        <div v-for="(block, bi) in section.blocks" :key="block.id" class="ml-4 mb-3 p-3 border rounded bg-gray-50">
          <div class="flex items-center justify-between mb-2">
            <select v-model="block.blockType" @change="updateBlock(block)"
              class="text-sm border rounded px-2 py-1">
              <option value="TEXT">Text</option>
              <option value="IMAGE">Image</option>
              <option value="AUDIO">Audio</option>
              <option value="TABLE">Table</option>
              <option value="QUESTION">Question</option>
              <option value="SUBMISSION">Submission</option>
            </select>
            <button @click="deleteBlock(block.id, section.id, bi)" class="text-sm text-red-600">Xóa</button>
          </div>

          <!-- Block editor by type -->
          <div v-if="block.blockType === 'TEXT'">
            <textarea v-model="blockData[block.id].content" @change="saveBlock(block)"
              class="w-full border rounded px-3 py-2 min-h-[100px]" placeholder="Nhập nội dung text (HTML)..."></textarea>
          </div>

          <div v-else-if="block.blockType === 'IMAGE'">
            <div class="flex gap-2 items-start">
              <input v-model="blockData[block.id].imageUrl" @change="saveBlock(block)"
                class="flex-1 border rounded px-2 py-1 text-sm" placeholder="URL hình ảnh..." />
              <label class="px-3 py-1 bg-blue-100 text-blue-700 rounded text-sm cursor-pointer">
                Upload
                <input type="file" accept="image/*" class="hidden" @change="uploadFile($event, block)" />
              </label>
            </div>
            <input v-model="blockData[block.id].caption" @change="saveBlock(block)"
              class="mt-2 w-full border rounded px-2 py-1 text-sm" placeholder="Chú thích..." />
            <img v-if="blockData[block.id].imageUrl" :src="blockData[block.id].imageUrl" class="mt-2 max-h-32 rounded" />
          </div>

          <div v-else-if="block.blockType === 'AUDIO'">
            <div class="flex gap-2 items-start">
              <input v-model="blockData[block.id].audioUrl" @change="saveBlock(block)"
                class="flex-1 border rounded px-2 py-1 text-sm" placeholder="URL audio..." />
              <label class="px-3 py-1 bg-blue-100 text-blue-700 rounded text-sm cursor-pointer">
                Upload
                <input type="file" accept="audio/*" class="hidden" @change="uploadFile($event, block)" />
              </label>
            </div>
            <input v-model="blockData[block.id].transcript" @change="saveBlock(block)"
              class="mt-2 w-full border rounded px-2 py-1 text-sm" placeholder="Transcript (tùy chọn)..." />
            <audio v-if="blockData[block.id].audioUrl" :src="blockData[block.id].audioUrl" controls class="mt-2 w-full"></audio>
          </div>

          <div v-else-if="block.blockType === 'TABLE'">
            <div class="space-y-2">
              <div>
                <label class="text-xs text-gray-500">Headers (comma-separated):</label>
                <input v-model="blockData[block.id].headersStr" @change="saveBlock(block)"
                  class="w-full border rounded px-2 py-1 text-sm" placeholder="col1, col2, col3" />
              </div>
              <div>
                <label class="text-xs text-gray-500">Rows (mỗi dòng là 1 row, cells cách nhau bởi |):</label>
                <textarea v-model="blockData[block.id].rowsStr" @change="saveBlock(block)"
                  class="w-full border rounded px-2 py-1 text-sm min-h-[60px]" placeholder="a | b | c"></textarea>
              </div>
            </div>
          </div>

          <div v-else-if="block.blockType === 'QUESTION'">
            <div class="space-y-2">
              <input v-model="blockData[block.id].questionText" @change="saveBlock(block)"
                class="w-full border rounded px-2 py-1 text-sm" placeholder="Câu hỏi..." />
              <div class="flex gap-2">
                <label class="text-xs text-gray-500">Loại:</label>
                <select v-model="blockData[block.id].questionType" @change="saveBlock(block)"
                  class="text-sm border rounded px-2 py-1">
                  <option value="MULTIPLE_CHOICE">Multiple Choice</option>
                  <option value="FILL_IN_BLANK">Fill in Blank</option>
                  <option value="TRUE_FALSE">True/False</option>
                  <option value="MATCHING">Matching</option>
                </select>
              </div>
              <div v-if="blockData[block.id].questionType === 'MULTIPLE_CHOICE'">
                <label class="text-xs text-gray-500">Options (mỗi option 1 dòng):</label>
                <textarea v-model="blockData[block.id].optionsStr" @change="saveBlock(block)"
                  class="w-full border rounded px-2 py-1 text-sm min-h-[60px]" placeholder="Option A"></textarea>
              </div>
              <input v-model="blockData[block.id].correctAnswer" @change="saveBlock(block)"
                class="w-full border rounded px-2 py-1 text-sm" placeholder="Đáp án đúng..." />
              <input v-model="blockData[block.id].explanation" @change="saveBlock(block)"
                class="w-full border rounded px-2 py-1 text-sm" placeholder="Giải thích (tùy chọn)..." />
              <div class="flex gap-2">
                <input v-model="blockData[block.id].imageUrl" @change="saveBlock(block)"
                  class="flex-1 border rounded px-2 py-1 text-sm" placeholder="URL hình ảnh (tùy chọn)..." />
                <label class="px-3 py-1 bg-blue-100 text-blue-700 rounded text-sm cursor-pointer">
                  <input type="file" accept="image/*" class="hidden" @change="uploadFile($event, block, 'image')" />Ảnh
                </label>
              </div>
              <div class="flex gap-2">
                <input v-model="blockData[block.id].audioUrl" @change="saveBlock(block)"
                  class="flex-1 border rounded px-2 py-1 text-sm" placeholder="URL audio (tùy chọn)..." />
                <label class="px-3 py-1 bg-blue-100 text-blue-700 rounded text-sm cursor-pointer">
                  <input type="file" accept="audio/*" class="hidden" @change="uploadFile($event, block, 'audio')" />Audio
                </label>
              </div>
            </div>
          </div>

          <div v-else-if="block.blockType === 'SUBMISSION'">
            <div class="space-y-2">
              <textarea v-model="blockData[block.id].prompt" @change="saveBlock(block)"
                class="w-full border rounded px-2 py-1 text-sm min-h-[60px]" placeholder="Yêu cầu bài tập..."></textarea>
              <select v-model="blockData[block.id].submissionType" @change="saveBlock(block)"
                class="text-sm border rounded px-2 py-1">
                <option value="TEXT">Text</option>
                <option value="AUDIO">Audio</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <!-- Add Section -->
      <button @click="addSection" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
        + Thêm Section
      </button>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { useRoute } from 'vue-router';
import lessonStructureService from '../../services/lessonStructureService';
import lessonService from '../../services/lessonService';

const route = useRoute();
const lessonId = Number(route.params.id);
const lesson = ref(null);
const sections = ref([]);
const loading = ref(true);
const blockData = reactive({});

onMounted(async () => {
  await loadLesson();
  await loadStructure();
  loading.value = false;
});

async function loadLesson() {
  try {
    const lessons = await lessonService.getAll();
    lesson.value = lessons.find(l => l.id === lessonId);
  } catch (e) {
    console.error('Failed to load lesson', e);
  }
}

async function loadStructure() {
  try {
    sections.value = await lessonStructureService.getAdminStructure(lessonId);
    sections.value.forEach(s => {
      s.blocks.forEach(b => {
        try { blockData[b.id] = JSON.parse(b.data); } catch { blockData[b.id] = { content: b.data || '' }; }
        normalizeBlockData(blockData[b.id]);
      });
    });
  } catch (e) {
    console.error('Failed to load structure', e);
    sections.value = [];
  }
}

function normalizeBlockData(d) {
  if (d.options && Array.isArray(d.options)) d.optionsStr = d.options.join('\n');
  if (d.headers && Array.isArray(d.headers)) d.headersStr = d.headers.join(', ');
  if (d.rows && Array.isArray(d.rows)) d.rowsStr = d.rows.map(r => r.join(' | ')).join('\n');
}

function serializeBlockData(d) {
  const out = { ...d };
  if (out.optionsStr) {
    out.options = out.optionsStr.split('\n').filter(s => s.trim());
    delete out.optionsStr;
  }
  if (out.headersStr) {
    out.headers = out.headersStr.split(',').map(s => s.trim()).filter(s => s);
    delete out.headersStr;
  }
  if (out.rowsStr) {
    out.rows = out.rowsStr.split('\n').filter(s => s.trim()).map(r => r.split('|').map(c => c.trim()));
    delete out.rowsStr;
  }
  delete out._saveTimer;
  return out;
}

function debounceSave(block) {
  if (blockData[block.id]._saveTimer) clearTimeout(blockData[block.id]._saveTimer);
  blockData[block.id]._saveTimer = setTimeout(() => saveBlock(block), 500);
}

async function saveBlock(block) {
  const data = serializeBlockData(blockData[block.id]);
  try {
    await lessonStructureService.updateBlock(block.id, {
      blockType: block.blockType,
      data: JSON.stringify(data)
    });
  } catch (e) {
    console.error('Failed to save block', e);
  }
}

async function updateBlock(block) {
  const data = serializeBlockData(blockData[block.id]);
  try {
    await lessonStructureService.updateBlock(block.id, {
      blockType: block.blockType,
      data: JSON.stringify(data)
    });
  } catch (e) {
    console.error('Failed to update block', e);
  }
}

async function uploadFile(event, block, field = 'imageUrl') {
  const file = event.target.files[0];
  if (!file) return;
  try {
    const result = await lessonStructureService.uploadFile(file);
    blockData[block.id][field] = result.url;
    await saveBlock(block);
  } catch (e) {
    console.error('Upload failed', e);
  }
}

async function addSection() {
  try {
    const section = await lessonStructureService.addSection(lessonId, {
      title: 'Section mới',
      orderIndex: (sections.value.length + 1) * 10
    });
    section.blocks = [];
    sections.value.push(section);
  } catch (e) {
    console.error('Failed to add section', e);
  }
}

async function updateSection(section) {
  try {
    await lessonStructureService.updateSection(section.id, {
      title: section.title,
      orderIndex: section.orderIndex
    });
  } catch (e) {
    console.error('Failed to update section', e);
  }
}

async function deleteSection(sectionId, index) {
  try {
    await lessonStructureService.deleteSection(sectionId);
    sections.value.splice(index, 1);
  } catch (e) {
    console.error('Failed to delete section', e);
  }
}

async function addBlock(sectionId) {
  try {
    const section = await lessonStructureService.addBlock(sectionId, {
      blockType: 'TEXT',
      data: JSON.stringify({ content: '' }),
      orderIndex: 10
    });
    const idx = sections.value.findIndex(s => s.id === sectionId);
    if (idx !== -1) sections.value[idx] = section;
    section.blocks.forEach(b => {
      if (!blockData[b.id]) {
        try { blockData[b.id] = JSON.parse(b.data); } catch { blockData[b.id] = { content: '' }; }
      }
      normalizeBlockData(blockData[b.id]);
    });
  } catch (e) {
    console.error('Failed to add block', e);
  }
}

async function deleteBlock(blockId, sectionId, index) {
  try {
    await lessonStructureService.deleteBlock(blockId);
    const section = sections.value.find(s => s.id === sectionId);
    if (section) section.blocks.splice(index, 1);
  } catch (e) {
    console.error('Failed to delete block', e);
  }
}
</script>
```

---

### Task 8: Add Router & Navigation

**Files:**
- Modify: `frontend/src/router/index.js`

- [ ] **Step 1: Add admin lesson builder route**

In `frontend/src/router/index.js`, add the route for the lesson builder (inside the admin children):

```javascript
{
  path: 'lessons/:id/build',
  name: 'AdminLessonBuilder',
  component: () => import('../views/admin/AdminLessonBuilder.vue'),
  meta: { requiresAuth: true, role: 'ADMIN' }
}
```

Also add a route for the student preview:

```javascript
{
  path: '/lessons/:id/preview',
  name: 'LessonPreview',
  component: () => import('../views/lessons/LessonPreview.vue'),
  meta: { requiresAuth: true }
}
```

- [ ] **Step 2: Add link on admin lessons page**

In `frontend/src/views/admin/AdminLessons.vue`, add a "Build" button in the actions column:
```html
<button @click="$router.push('/admin/lessons/' + lesson.id + '/build')"
  class="text-sm px-2 py-1 bg-purple-100 text-purple-700 rounded">
  Build
</button>
```

---

### Task 9: Student Preview Page

**Files:**
- Create: `frontend/src/views/lessons/LessonPreview.vue`

- [ ] **Step 1: Create LessonPreview.vue**

```vue
<template>
  <div class="max-w-4xl mx-auto p-6">
    <h1 class="text-2xl font-bold mb-2">{{ lesson?.title }}</h1>
    <p class="text-gray-500 mb-6">{{ lesson?.description }}</p>
    <LessonBlockRenderer v-if="sections.length" :sections="sections" />
    <p v-else class="text-gray-400 italic">Bài học chưa có nội dung.</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import lessonService from '../../services/lessonService';
import lessonStructureService from '../../services/lessonStructureService';
import LessonBlockRenderer from '../../components/lessons/block-renderer/LessonBlockRenderer.vue';

const route = useRoute();
const lessonId = Number(route.params.id);
const lesson = ref(null);
const sections = ref([]);

onMounted(async () => {
  try {
    const lessons = await lessonService.getAll();
    lesson.value = lessons.find(l => l.id === lessonId);
  } catch {}
  try {
    sections.value = await lessonStructureService.getStructure(lessonId);
  } catch {}
});
</script>
```

---

### Task 10: Build & Test

- [ ] **Step 1: Rebuild backend**

```bash
cd C:\Users\ASUS\Documents\LAPTRINH\engflow
mvn compile -q
```

- [ ] **Step 2: Verify frontend compiles**

```bash
cd C:\Users\ASUS\Documents\LAPTRINH\engflow\frontend
npm run build -- --mode development 2>&1
```

- [ ] **Step 3: Start backend and frontend, then test manually**
