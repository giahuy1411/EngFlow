package com.datn.engflow.service;

/**
 * interface TtsService.
 */
public interface TtsService {
    byte[] synthesize(String text, String voice, String lang);
    boolean isAvailable();
}
