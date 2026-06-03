package com.datn.engflow.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckRequest {
    
    @NotBlank(message = "Tên bộ từ vựng không được để trống")
    @Size(max = 200, message = "Tên bộ từ vựng tối đa 200 ký tự")
    private String name;
    
    private String description;
    
    @Size(max = 50, message = "Nguồn tối đa 50 ký tự")
    private String source;
    
    @Size(max = 10, message = "CEFR level tối đa 10 ký tự")
    private String cefrLevel;
    
    @Builder.Default
    private Boolean isPublic = true;
    
    @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
    private String thumbnailUrl;
}
