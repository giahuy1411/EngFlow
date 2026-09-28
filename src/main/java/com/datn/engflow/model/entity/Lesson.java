package com.datn.engflow.model.entity;

import com.datn.engflow.model.enums.LessonLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bài học chính của hệ thống: nội dung đọc, từ vựng đi kèm và danh sách
 * {@link Exercise} người học làm.
 *
 * <p>Tầng entity. {@link LessonService} đọc/tạo/sửa qua {@code LessonRepository};
 * {@link AdminService} quản lý nội dung; {@link DeckService} và {@link SrsService}
 * dùng bộ từ gắn với lesson. {@link #vocabularies} được đánh dấu {@code @JsonIgnore}
 * nên bài học luôn được đọc qua projection nhẹ, tránh kéo theo cả list từ.
 */
@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_id")
    private Long id;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;

    // Bản gốc chỉ đọc trước khi HtmlCleanupMigration dọn HTML: migration lấy
    // contentOriginal làm nguồn, dọn rồi ghi đè content, nên mỗi lần chạy lại không
    // dọc chồng lên bản đã sạch. Không có writer nào khác đụng tới cột này.
    @Column(name = "content_original", columnDefinition = "NVARCHAR(MAX)")
    private String contentOriginal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonLevel level;

    @Column(length = 50)
    private String category;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", length = 20)
    private com.datn.engflow.model.enums.SkillType skillType;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Builder.Default
    @Column(name = "is_published")
    private Boolean isPublished = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @org.hibernate.annotations.BatchSize(size = 20)
    @Builder.Default
    private List<Vocabulary> vocabularies = new ArrayList<>();
}
