package com.datn.engflow.model.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonSubmissionDTO {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private Long lessonId;
    private String lessonTitle;
    private String skillType;
    private String submissionText;
    private String audioUrl;
    private Double score;
    private String feedback;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
