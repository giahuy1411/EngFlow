package com.datn.engflow.service;

/**
 * Text-to-speech abstraction for generating the audio a LISTENING exercise
 * speaks. The only implementation in the project is
 * {@link SupertonicProxyTtsService}, a proxy to the supertonic sidecar; the
 * interface exists so a cloud TTS vendor can be swapped in without touching
 * callers.
 */
public interface TtsService {

    /**
     * Renders {@code text} as audio.
     *
     * @param text  the text to speak
     * @param voice the voice id, or null to let the backend pick its default
     * @param lang  the language hint, or null to let the backend default
     * @return the audio bytes, or null when synthesis failed or was unavailable
     */
    byte[] synthesize(String text, String voice, String lang);

    /**
     * Probes whether the speech backend is reachable right now.
     *
     * @return true when a synthesis request would currently succeed
     */
    boolean isAvailable();
}
