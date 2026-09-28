package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.AdminStatsDTO;
import com.datn.engflow.model.dto.response.AdminUserDTO;
import com.datn.engflow.model.dto.response.LessonSummaryDTO;
import com.datn.engflow.model.entity.*;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.SkillType;
import com.datn.engflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Backend của khu vực quản trị: thống kê dashboard, quản lý user (premium/active/admin)
 * và CRUD bài học/từ vựng.
 *
 * <p>Tầng service, được gọi từ các controller {@code Admin*}. Dữ liệu lấy qua
 * {@link UserRepository}, {@link LessonRepository}, {@link VocabularyRepository},
 * {@link ExerciseRepository} và {@link LessonSubmissionRepository}; phần xóa bài học ủy
 * quyền cho {@link LessonService}. Streak của user lấy từ {@link StreakService}, tức bảng
 * {@code study_days} — không phải cột {@code users.last_study_date} đã ngừng cập nhật.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;
    private final ExerciseRepository exerciseRepository;
    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final LessonService lessonService;
    private final Clock clock;
    private final StreakService streakService;

    /**
     * Số liệu tổng quan cho dashboard admin.
     *
     * <p>{@code recentUsers} đếm số người học thực tế trong 7 ngày gần nhất qua
     * {@link StreakService#countActiveLearnersInLastDays} (bảng {@code study_days}), không
     * dùng cột {@code users.last_study_date} đã lỗi thời.
     *
     * @return DTO chứa các con số đếm toàn hệ thống
     */
    public AdminStatsDTO getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalLessons = lessonRepository.count();
        long totalVocabulary = vocabularyRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        // audit-v13 F-13-08: was userRepository.countByLastStudyDateAfter(...), reading the
        // legacy users.last_study_date column that the streak refactor stopped maintaining
        // (measured 2026-09-22: user 2 had last_study_date=2026-09-19 while studying on
        // 2026-09-22). Count from the real study_days calendar instead.
        long recentUsers = streakService.countActiveLearnersInLastDays(7);
        long totalExercises = exerciseRepository.count();
        long totalSubmissions = lessonSubmissionRepository.count();

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalLessons(totalLessons)
                .totalVocabulary(totalVocabulary)
                .activeUsers(activeUsers)
                .recentUsers(recentUsers)
                .totalExercises(totalExercises)
                .totalSubmissions(totalSubmissions)
                .build();
    }

    /**
     * Danh sách user phân trang cho admin, kèm streak hiệu lực của từng người.
     *
     * <p>Streak của cả trang được đọc trong MỘT query qua
     * {@link StreakService#currentStreaks}; gọi {@code getCurrentStreak} theo từng dòng sẽ
     * thành N+1 (mỗi lần đọc {@code study_days} một lượt). Trang rỗng thì bỏ qua hẳn tầng streak.
     *
     * @param keyword từ khóa lọc theo username/email; null hoặc rỗng nghĩa là lấy tất cả
     * @param page chỉ số trang, 0-based
     * @param size số dòng mỗi trang, bị chặn trong khoảng 1..100
     * @return trang các {@link AdminUserDTO} đã gắn streak
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getAllUsers(String keyword, int page, int size) {
        Pageable pageable = adminPageRequest(page, size, Sort.by("createdAt").descending().and(Sort.by("id")));
        Page<User> users = (keyword == null || keyword.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.searchByKeywordPage(keyword.trim(), pageable);
        // Streak cho cả trang trong MỘT query: gọi getCurrentStreak theo từng row là
        // N+1 (mỗi call đọc study_days một lần). Trang rỗng thì không chạm tầng streak.
        List<Long> userIds = users.getContent().stream().map(User::getId).toList();
        Map<Long, Integer> streaks = userIds.isEmpty() ? Map.of() : streakService.currentStreaks(userIds);
        return users.map(u -> mapToAdminUserDTO(u, streaks));
    }

    /**
     * Bật/tắt premium cho một user. Khi bật, hạn được đặt 30 ngày kể từ hôm nay.
     *
     * @param userId id user cần đổi trạng thái
     * @return user sau khi lưu, đã gắn streak
     * @throws ResourceNotFoundException nếu không tìm thấy user
     */
    @Transactional
    public AdminUserDTO toggleUserPremium(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        boolean currentlyPremium = Boolean.TRUE.equals(user.getIsPremium());
        if (currentlyPremium) {
            user.setIsPremium(false);
            user.setPremiumExpiry(null);
        } else {
            user.setIsPremium(true);
            user.setPremiumExpiry(LocalDate.now(clock).plusDays(30));
        }
        User saved = userRepository.save(user);
        return mapToAdminUserDTO(saved);
    }

    /**
     * Thu hồi premium ngay lập tức (khác {@link #toggleUserPremium} ở chỗ không bật lại).
     *
     * @param userId id user cần thu hồi
     * @return user sau khi lưu, đã gắn streak
     * @throws ResourceNotFoundException nếu không tìm thấy user
     */
    @Transactional
    public AdminUserDTO revokeUserPremium(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsPremium(false);
        user.setPremiumExpiry(null);
        User saved = userRepository.save(user);
        return mapToAdminUserDTO(saved);
    }

    /**
     * Đảo trạng thái hoạt động (isActive) của user.
     *
     * @param userId id user cần đổi
     * @return user sau khi lưu, đã gắn streak
     * @throws ResourceNotFoundException nếu không tìm thấy user
     */
    @Transactional
    public AdminUserDTO toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        User saved = userRepository.save(user);
        return mapToAdminUserDTO(saved);
    }

    /**
     * Đảo quyền admin của user.
     *
     * @param userId id user cần đổi
     * @return user sau khi lưu, đã gắn streak
     * @throws ResourceNotFoundException nếu không tìm thấy user
     */
    @Transactional
    public AdminUserDTO toggleUserAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsAdmin(!Boolean.TRUE.equals(user.getIsAdmin()));
        User saved = userRepository.save(user);
        return mapToAdminUserDTO(saved);
    }

    /** Đường một-user (4 method toggle*) — đọc streak trực tiếp, không cần batch. */
    private AdminUserDTO mapToAdminUserDTO(User u) {
        return mapToAdminUserDTO(u, Map.of());
    }

    /**
     * @param streaks kết quả batch cho cả trang; user vắng mặt (chưa từng học) nhận 0.
     *                Với đường một-user, map rỗng nên phải đọc trực tiếp.
     */
    private AdminUserDTO mapToAdminUserDTO(User u, Map<Long, Integer> streaks) {
        Integer currentStreak = streaks.isEmpty()
                ? streakService.getCurrentStreak(u.getId())
                : streaks.getOrDefault(u.getId(), 0);
        return AdminUserDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .isAdmin(u.getIsAdmin())
                .isActive(u.getIsActive())
                .totalPoints(u.getTotalPoints())
                .currentStreak(currentStreak)
                .createdAt(u.getCreatedAt())
                .isPremium(Boolean.TRUE.equals(u.getIsPremium()))
                .premiumExpiry(u.getPremiumExpiry())
                .build();
    }

    // Lessons
    /**
     * Bản không lọc level của {@link #getAllLessonsAdmin(String, LessonLevel, int, int)}.
     *
     * @param keyword từ khóa tìm theo tiêu đề; null/rỗng là không lọc
     * @param page chỉ số trang, 0-based
     * @param size số dòng mỗi trang
     * @return trang tóm tắt bài học
     */
    public Page<LessonSummaryDTO> getAllLessonsAdmin(String keyword, int page, int size) {
        return getAllLessonsAdmin(keyword, null, page, size);
    }

    /**
     * Danh sách bài học cho màn admin, lọc theo từ khóa và level, sắp theo orderIndex.
     *
     * @param keyword từ khóa tìm theo tiêu đề; null/rỗng là không lọc
     * @param level level cần lọc; null là mọi level
     * @param page chỉ số trang, 0-based
     * @param size số dòng mỗi trang
     * @return trang tóm tắt bài học
     */
    public Page<LessonSummaryDTO> getAllLessonsAdmin(String keyword, LessonLevel level, int page, int size) {
        Pageable pageable = adminPageRequest(page, size, Sort.by("orderIndex").ascending().and(Sort.by("id")));
        return getAllLessonsAdmin(keyword, level, pageable);
    }

    /**
     * Bản không lọc level của {@link #getAllLessonsAdmin(String, LessonLevel, Pageable)}.
     *
     * @param keyword từ khóa tìm theo tiêu đề; null/rỗng là không lọc
     * @param pageable thông số phân trang do caller quyết định
     * @return trang tóm tắt bài học
     */
    public Page<LessonSummaryDTO> getAllLessonsAdmin(String keyword, Pageable pageable) {
        return getAllLessonsAdmin(keyword, null, pageable);
    }

    /**
     * Truy vấn nền của các overload {@code getAllLessonsAdmin}: đẩy lọc xuống SQL rồi map sang DTO.
     *
     * @param keyword từ khóa tìm theo tiêu đề; null/rỗng là không lọc
     * @param level level cần lọc; null là mọi level
     * @param pageable thông số phân trang
     * @return trang tóm tắt bài học
     */
    public Page<LessonSummaryDTO> getAllLessonsAdmin(String keyword, LessonLevel level, Pageable pageable) {
        Page<Lesson> lessons = lessonRepository.findAdminPage(
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                level,
                pageable);
        return lessons.map(this::toSummary);
    }

    /** Map entity Lesson sang DTO tóm tắt dùng cho danh sách admin. */
    private LessonSummaryDTO toSummary(Lesson lesson) {
        return LessonSummaryDTO.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .level(lesson.getLevel())
                .category(lesson.getCategory())
                .durationMinutes(lesson.getDurationMinutes())
                .thumbnailUrl(lesson.getThumbnailUrl())
                .skillType(lesson.getSkillType())
                .orderIndex(lesson.getOrderIndex())
                .isPublished(lesson.getIsPublished())
                .createdAt(lesson.getCreatedAt())
                .build();
    }

    /**
     * Đọc một bài học theo id (không kiểm tra published — dùng cho admin).
     *
     * @param id id bài học
     * @return entity bài học
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    public Lesson getLesson(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
    }

    /**
     * Tạo bài học mới từ request của admin.
     *
     * <p>Mặc định {@code isPublished=false} và {@code skillType=GRAMMAR} khi request không
     * cung cấp.
     *
     * @param request dữ liệu bài học
     * @return entity bài học đã lưu
     */
    @Transactional
    public Lesson createLesson(LessonRequest request) {
        Lesson lesson = Lesson.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .content(request.getContent())
                .level(request.getLevel())
                .category(request.getCategory())
                .durationMinutes(request.getDurationMinutes())
                .thumbnailUrl(request.getThumbnailUrl())
                .audioUrl(request.getAudioUrl())
                .orderIndex(request.getOrderIndex())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .skillType(request.getSkillType() != null ? SkillType.valueOf(request.getSkillType().toUpperCase()) : SkillType.GRAMMAR)
                .build();
        return lessonRepository.save(lesson);
    }

    /**
     * Cập nhật bài học. Chỉ đổi {@code isPublished} khi request có giá trị (null = giữ nguyên).
     *
     * @param id id bài học cần sửa
     * @param request dữ liệu mới
     * @return entity bài học đã lưu
     * @throws ResourceNotFoundException nếu không tìm thấy bài học
     */
    @Transactional
    public Lesson updateLesson(Long id, LessonRequest request) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setContent(request.getContent());
        lesson.setLevel(request.getLevel());
        lesson.setCategory(request.getCategory());
        lesson.setDurationMinutes(request.getDurationMinutes());
        lesson.setThumbnailUrl(request.getThumbnailUrl());
        lesson.setAudioUrl(request.getAudioUrl());
        lesson.setOrderIndex(request.getOrderIndex());
        if (request.getIsPublished() != null) {
            lesson.setIsPublished(request.getIsPublished());
        }
        return lessonRepository.save(lesson);
    }

    /**
     * Xóa bài học cùng toàn bộ dữ liệu con. Ủy quyền cho {@link LessonService#deleteLesson}.
     *
     * @param id id bài học cần xóa
     */
    @Transactional
    public void deleteLesson(Long id) {
        lessonService.deleteLesson(id);
    }

    /**
     * Đảo trạng thái published của bài học.
     *
     * @param id id bài học
     * @return entity bài học đã lưu
     * @throws ResourceNotFoundException nếu không tìm thấy bài học
     */
    @Transactional
    public Lesson toggleLessonPublish(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        lesson.setIsPublished(!Boolean.TRUE.equals(lesson.getIsPublished()));
        return lessonRepository.save(lesson);
    }

    // Vocabulary
    /**
     * Danh sách từ vựng phân trang cho admin, sắp theo id.
     *
     * @param page chỉ số trang, 0-based
     * @param size số dòng mỗi trang
     * @return trang các entity từ vựng
     */
    public Page<Vocabulary> getAllVocabulary(int page, int size) {
        Pageable pageable = adminPageRequest(page, size, Sort.by("id").ascending());
        return vocabularyRepository.findAll(pageable);
    }

    /**
     * Tạo từ vựng mới. Nếu request có {@code lessonId} thì gắn từ vào bài học đó.
     *
     * @param request dữ liệu từ vựng
     * @return entity từ vựng đã lưu
     * @throws ResourceNotFoundException nếu {@code lessonId} được cung cấp nhưng không tồn tại
     */
    @Transactional
    public Vocabulary createVocabulary(VocabularyRequest request) {
        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }
        Vocabulary vocabulary = Vocabulary.builder()
                .word(request.getWord())
                .pronunciation(request.getPronunciation())
                .meaning(request.getMeaning())
                .definitionEn(request.getDefinitionEn())
                .exampleSentence(request.getExampleSentence())
                .wordType(request.getWordType())
                .cefrLevel(request.getCefrLevel())
                .source(request.getSource())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .lesson(lesson)
                .build();
        return vocabularyRepository.save(vocabulary);
    }

    /**
     * Cập nhật từ vựng. {@code lessonId} null sẽ gỡ liên kết bài học của từ.
     *
     * @param id id từ vựng cần sửa
     * @param request dữ liệu mới
     * @return entity từ vựng đã lưu
     * @throws ResourceNotFoundException nếu không tìm thấy từ vựng hoặc lesson được trỏ tới
     */
    @Transactional
    public Vocabulary updateVocabulary(Long id, VocabularyRequest request) {
        Vocabulary vocabulary = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary", "id", id));
        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }
        vocabulary.setWord(request.getWord());
        vocabulary.setPronunciation(request.getPronunciation());
        vocabulary.setMeaning(request.getMeaning());
        vocabulary.setDefinitionEn(request.getDefinitionEn());
        vocabulary.setExampleSentence(request.getExampleSentence());
        vocabulary.setWordType(request.getWordType());
        vocabulary.setCefrLevel(request.getCefrLevel());
        vocabulary.setSource(request.getSource());
        vocabulary.setAudioUrl(request.getAudioUrl());
        vocabulary.setImageUrl(request.getImageUrl());
        vocabulary.setLesson(lesson);
        return vocabularyRepository.save(vocabulary);
    }

    /**
     * Xóa một từ vựng theo id.
     *
     * @param id id từ vựng cần xóa
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    @Transactional
    public void deleteVocabulary(Long id) {
        Vocabulary vocabulary = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary", "id", id));
        vocabularyRepository.delete(vocabulary);
    }

    // Chặn size xuống 1..MAX_PAGE_SIZE và page âm: tham số đến từ query string nên không tin được,
    // một size khổng lồ sẽ kéo cả bảng vào bộ nhớ.
    private Pageable adminPageRequest(int page, int size, Sort sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, sort);
    }

    // (Exercises section removed)
}
