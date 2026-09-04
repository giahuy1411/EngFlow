package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine;
import com.datn.engflow.model.entity.VideoAttempt;
import com.datn.engflow.model.entity.VideoLesson;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.VideoAttemptRepository;
import com.datn.engflow.service.assessment.LlmChatClient;
import com.datn.engflow.service.assessment.SpeakingAssessmentService;
import com.datn.engflow.service.assessment.TranscriptAlignmentMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI grading for shadowing attempts: MinIO audio → Whisper transcript →
 * alignment against the target sentence (score) → LLM feedback text.
 *
 * <p>The score is deterministic (coverage-based, thang 10) so it never
 * hallucinates; the LLM only writes the Vietnamese feedback sentence and is
 * allowed to fail (fallback feedback is used instead). A teacher can still
 * override everything via manual grading.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShadowingAiGradingService {

    private static final String SYSTEM_PROMPT = """
            Bạn là giáo viên tiếng Việt dạy tiếng Anh, đang nhận xét bài luyện shadowing của học viên.
            Bạn nhận được: câu tiếng Anh mẫu và văn bản mà máy nhận diện được từ bản ghi của học viên.
            Chỉ nhận xét về độ chính xác nội dung: từ nào nói đúng, từ nào thiếu hoặc sai, và một gợi ý cụ thể.
            Bạn không nghe được âm thanh, TUYỆT ĐỐI KHÔNG nhận xét về phát âm hay giọng nói.

            QUY TẮC BẮT BUỘC: Viết nhận xét bằng TIẾNG VIỆT, 1-2 câu, có dấu tiếng Việt đầy đủ.
            Trả về CHỖ văn bản nhận xét, không JSON, không ngoặc kép, không tiếng Anh.

            Ví dụ: "Bạn nói đúng 8/10 từ trong câu. Từ 'coffee' bị thiếu, hãy nghe lại và nhấn rõ từ này nhé."
            """;

    private final VideoAttemptRepository attemptRepository;
    private final VideoLessonService videoLessonService;
    private final MinioService minioService;
    private final SpeakingAssessmentService assessmentService;
    private final LlmChatClient llm;

    /**
     * Grades one attempt with AI and stores score + feedback. Existing manual
     * grades (GRADED with a teacher) are not touched.
     */
    @Transactional
    public VideoAttempt aiGrade(Long attemptId) {
        VideoAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAttempt", "id", attemptId));
        if ("GRADED".equals(attempt.getStatus()) && attempt.getGradedBy() != null) {
            throw new BadRequestException("Bản ghi này đã được giáo viên chấm, không chấm lại bằng AI");
        }

        String referenceText = referenceText(attempt);
        String transcript = assessmentService.transcribe(attempt.getMediaObjectKey(), attempt.getMediaType());

        TranscriptAlignmentMetrics.AlignmentResult alignment =
                TranscriptAlignmentMetrics.compute(referenceText, transcript);
        if (transcript == null || transcript.isBlank()) {
            // Whisper heard nothing — score 0 with explicit feedback instead of crashing.
            attempt.setScore(BigDecimal.ZERO);
            attempt.setAdminFeedback("AI không nghe rõ bản ghi âm. Hãy ghi lại ở nơi yên tĩnh và nói to, rõ hơn.");
            attempt.setStatus("GRADED");
            attempt.setGradedAt(LocalDateTime.now());
            return attemptRepository.save(attempt);
        }

        attempt.setScore(coverageScore(alignment));
        attempt.setAdminFeedback(buildFeedback(referenceText, transcript, alignment));
        attempt.setStatus("GRADED");
        attempt.setGradedAt(LocalDateTime.now());
        return attemptRepository.save(attempt);
    }

    private String referenceText(VideoAttempt attempt) {
        VideoLesson lesson = attempt.getVideoLesson();
        List<TranscriptLine> transcript = videoLessonService.readTranscript(lesson.getTranscriptJson());
        int index = attempt.getLineIndex();
        if (index < 0 || index >= transcript.size()) {
            throw new BadRequestException("Dòng phụ đề không hợp lệ");
        }
        return transcript.get(index).textEn();
    }

    /** Coverage (0-100) maps linearly onto the 0-10 scale, one decimal. */
    static BigDecimal coverageScore(TranscriptAlignmentMetrics.AlignmentResult alignment) {
        return BigDecimal.valueOf(alignment.coveragePercent())
                .divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);
    }

    private String buildFeedback(String referenceText, String transcript,
                                 TranscriptAlignmentMetrics.AlignmentResult alignment) {
        double coverage = alignment.coveragePercent();
        try {
            String userPrompt = "Câu mẫu: " + referenceText + "\n"
                    + "Văn bản máy nghe được: " + transcript + "\n"
                    + "Thống kê: đúng " + alignment.correctWords() + " từ, thiếu "
                    + alignment.deletions() + " từ, thừa " + alignment.insertions() + " từ (độ phủ "
                    + String.format(java.util.Locale.ROOT, "%.0f%%", coverage) + ").\n"
                    + "Viết nhận xét tiếng Việt cho học viên.";
            String feedback = llm.completeWithRetry(SYSTEM_PROMPT, userPrompt).trim();
            if (!feedback.isEmpty() && hasVietnameseLetters(feedback)) {
                return feedback;
            }
            log.warn("Shadowing LLM feedback ignored (empty or non-Vietnamese)");
        } catch (Exception ex) {
            log.warn("Shadowing LLM feedback failed: {}", ex.getMessage());
        }
        return coverageFallbackFeedback(alignment);
    }

    /** Rejects accidental English output from the small model. */
    static boolean hasVietnameseLetters(String text) {
        return text != null && text.chars().anyMatch(c ->
                (c >= 0x00C0 && c <= 0x1EF9) || c == 0x0110 || c == 0x0111);
    }

    static String coverageFallbackFeedback(TranscriptAlignmentMetrics.AlignmentResult alignment) {
        double coverage = alignment.coveragePercent();
        if (coverage >= 90) {
            return "Bạn nói đúng gần như toàn bộ câu mẫu. Thử tăng tốc độ theo giọng trong video nhé.";
        }
        if (coverage >= 60) {
            return "Bạn bắt được phần lớn nội dung (" + String.format(java.util.Locale.ROOT, "%.0f%%", coverage)
                    + "). Nghe lại các từ bị thiếu rồi nói thử lần nữa.";
        }
        return "Bạn mới bắt được " + String.format(java.util.Locale.ROOT, "%.0f%%", coverage)
                + " nội dung câu mẫu. Nghe chậm từng từ rồi lặp lại nhé.";
    }
}
