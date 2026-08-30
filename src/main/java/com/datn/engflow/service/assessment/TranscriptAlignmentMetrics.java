package com.datn.engflow.service.assessment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure-text alignment metrics between a reference script and a recognized transcript.
 *
 * <p>The metrics are deliberately conservative: they measure content coverage and
 * word-level deviation, not pronunciation quality. Callers must not label them as
 * pronunciation scores.</p>
 */
public final class TranscriptAlignmentMetrics {

    private static final int MAX_EDIT_DISTANCE_CELLS = 4_000_000;

    private TranscriptAlignmentMetrics() {
    }

    /**
     * Computes alignment metrics between reference and hypothesis word sequences.
     *
     * @param referenceText expected script (may be {@code null} for unscripted tasks)
     * @param transcript    recognized speech (may be {@code null} or blank)
     * @return metrics with word error rate, reference coverage, and edit breakdown
     */
    public static AlignmentResult compute(String referenceText, String transcript) {
        List<String> reference = tokenize(referenceText);
        List<String> hypothesis = tokenize(transcript);

        if (reference.isEmpty()) {
            return new AlignmentResult(0.0, 0.0, 0, 0, 0, 0, hypothesis.size());
        }
        if (hypothesis.isEmpty()) {
            return new AlignmentResult(100.0, 0.0, reference.size(), 0, 0, reference.size(), 0);
        }

        int[][] distance = editDistanceMatrix(reference, hypothesis);
        int[] ops = backtraceOperations(distance, reference, hypothesis);
        int substitutions = ops[0];
        int insertions = ops[1];
        int deletions = ops[2];
        int correct = reference.size() - deletions - substitutions;

        double wer = 100.0 * (substitutions + insertions + deletions) / reference.size();
        double coverage = 100.0 * correct / reference.size();
        return new AlignmentResult(
                round(wer), round(coverage), correct, substitutions, insertions, deletions, hypothesis.size());
    }

    private static List<String> tokenize(String text) {
        List<String> words = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return words;
        }
        // Standard WER normalization: case-folded, punctuation (incl. apostrophes)
        // removed, so "Don't" and "dont" align as the same token.
        String normalized = text.toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("['\u2018\u2019`]", "");
        for (String raw : normalized.split("[^\\p{L}\\p{N}]+")) {
            if (!raw.isEmpty()) {
                words.add(raw);
            }
        }
        return words;
    }

    private static int[][] editDistanceMatrix(List<String> reference, List<String> hypothesis) {
        if ((long) (reference.size() + 1) * (hypothesis.size() + 1) > MAX_EDIT_DISTANCE_CELLS) {
            throw new IllegalArgumentException("Transcript too long for alignment metrics");
        }
        int[][] distance = new int[reference.size() + 1][hypothesis.size() + 1];
        for (int i = 0; i <= reference.size(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= hypothesis.size(); j++) {
            distance[0][j] = j;
        }
        for (int i = 1; i <= reference.size(); i++) {
            for (int j = 1; j <= hypothesis.size(); j++) {
                int cost = reference.get(i - 1).equals(hypothesis.get(j - 1)) ? 0 : 1;
                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + cost);
            }
        }
        return distance;
    }

    private static int[] backtraceOperations(int[][] distance, List<String> reference, List<String> hypothesis) {
        int substitutions = 0;
        int insertions = 0;
        int deletions = 0;
        int i = reference.size();
        int j = hypothesis.size();
        while (i > 0 && j > 0) {
            int current = distance[i][j];
            if (current == distance[i - 1][j - 1] + (reference.get(i - 1).equals(hypothesis.get(j - 1)) ? 0 : 1)) {
                if (!reference.get(i - 1).equals(hypothesis.get(j - 1))) {
                    substitutions++;
                }
                i--;
                j--;
            } else if (current == distance[i - 1][j] + 1) {
                deletions++;
                i--;
            } else {
                insertions++;
                j--;
            }
        }
        deletions += i;
        insertions += j;
        return new int[]{substitutions, insertions, deletions};
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * Alignment outcome for one submission.
     *
     * @param wordErrorRatePercent deviation from the reference script (0 = perfect)
     * @param coveragePercent      proportion of reference words spoken correctly
     * @param correctWords         matched reference words
     * @param substitutions        reference words replaced by other words
     * @param insertions           extra words not in the reference
     * @param deletions            reference words missing from the transcript
     * @param hypothesisWords      total recognized words
     */
    public record AlignmentResult(
            double wordErrorRatePercent,
            double coveragePercent,
            int correctWords,
            int substitutions,
            int insertions,
            int deletions,
            int hypothesisWords) {
    }
}
