package com.datn.engflow.service;

import com.datn.engflow.model.entity.Vocabulary;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AiVocabService {
    Mono<List<Vocabulary>> generateVocabByTopic(String topic, String cefrLevel, int count);
    Mono<Vocabulary> enrichWord(String word);
}
