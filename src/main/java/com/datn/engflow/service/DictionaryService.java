package com.datn.engflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Tra từ điển qua dictionaryapi.dev với Redis cache.
 * Tách khỏi controller để @Cacheable đi qua Spring proxy (self-invocation
 * không kích hoạt cache). Upstream đo thực tế ~20s từ mạng VN — cache 1h
 * bảo đảm mỗi từ chỉ trả giá một lần; timeout 30s chặn hang vô hạn.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DictionaryService {

    private final RestTemplate restTemplate;

    @Cacheable(value = "dictionary", key = "#clean", unless = "#result == '[]'")
    public String lookup(String clean) {
        try {
            String body = restTemplate.getForObject(
                    "https://api.dictionaryapi.dev/api/v2/entries/en/{w}", String.class, clean);
            return body == null ? "[]" : body;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return "[]";
        } catch (Exception e) {
            log.warn("Dictionary proxy failed for '{}': {}", clean, e.getMessage());
            return "[]";
        }
    }
}