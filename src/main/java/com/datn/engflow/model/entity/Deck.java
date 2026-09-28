package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bộ từ vựng gom nhiều {@link Vocabulary} qua bảng nối {@link DeckWord}, dùng cho
 * ôn SRS, trò chơi và các bài tập xếp từ theo chủ đề.
 *
 * <p>Tầng entity. {@link DeckService} đọc/ghi qua {@code DeckRepository} và
 * kiểm tra quyền theo {@link #owner} cùng {@link #isPublic}; {@link SrsService} và
 * {@link GameService} đọc danh sách từ qua {@link DeckWord} để dựng phiên ôn.
 * Deck có {@code owner == null} là deck hệ thống tạo sẵn, không ai sở hữu được,
 * nên mọi thao tác ghi của user đều bị chặn ở tầng service.
 */
@Entity
@Table(name = "decks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deck_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner; // null = system deck (pre-built)

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(length = 50)
    private String source; // "oxford3000", "awl", "toeic", "ielts", "thpt", "ai_generated", "user_created"

    @Column(name = "cefr_level", length = 10)
    private String cefrLevel;

    @Builder.Default
    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private List<DeckWord> words = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
