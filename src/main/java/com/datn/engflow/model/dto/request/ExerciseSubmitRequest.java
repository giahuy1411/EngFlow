package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSubmitRequest {

    @NotNull(message = "Mã bài tập không được để trống")
    private Long exerciseId;

    @NotBlank(message = "Câu trả lời không được để trống")
    private String userAnswer;
}
