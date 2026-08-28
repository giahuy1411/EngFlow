package com.datn.engflow.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class VocabularyDTO.
 */
public class VocabularyDTO {
    private Long id;
    private String word;
    private String pronunciation;
    private String meaning;
    private String exampleSentence;
    private String audioUrl;
    private String imageUrl;
    private String wordType;
}
