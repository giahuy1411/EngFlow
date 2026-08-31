package com.datn.engflow.service.assessment;

/**
 * Language-quality rubric produced by the local LLM from a transcript.
 *
 * <p>Scores use the 0-10 scale already stored in {@code score_grammar},
 * {@code score_vocabulary}, and {@code score_fluency}. The model never sees
 * audio, so these describe content, not pronunciation.</p>
 *
 * @param grammar    grammatical accuracy, 0-10
 * @param vocabulary range and appropriateness of word choice, 0-10
 * @param fluency    coherence and natural flow inferred from the transcript, 0-10
 * @param feedback   short learner-facing comment in Vietnamese
 */
public record SpeakingRubricResult(
        int grammar,
        int vocabulary,
        int fluency,
        String feedback) {

    /**
     * Averages the three rubric dimensions onto the same 0-10 scale the
     * teacher uses, rounded to one decimal.
     *
     * @return overall rubric score between 0.0 and 10.0
     */
    public double total() {
        return Math.round((grammar + vocabulary + fluency) / 3.0 * 10.0) / 10.0;
    }
}
