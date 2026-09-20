package com.datn.engflow.repository;

import com.datn.engflow.model.dto.projection.StudyDayOwner;
import com.datn.engflow.model.entity.StudyDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** SQL là nguồn lịch học thật; truy vấn luôn giới hạn ngày tương lai. */
public interface StudyDayRepository extends JpaRepository<StudyDay, Long> {
    boolean existsByUserIdAndStudyDate(Long userId, LocalDate studyDate);

    @Query("SELECT d.studyDate FROM StudyDay d WHERE d.userId = :userId "
            + "AND d.studyDate BETWEEN :start AND :end ORDER BY d.studyDate DESC")
    List<LocalDate> findDates(@Param("userId") Long userId,
                            @Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Mọi ngày học của nhiều user trong một khoảng — một query cho cả trang thay vì
     * một query mỗi row. Dùng cho leaderboard và danh sách user của admin, nơi mỗi
     * row cần streak riêng.
     */
    @Query("SELECT d.userId AS userId, d.studyDate AS studyDate FROM StudyDay d "
            + "WHERE d.userId IN :userIds AND d.studyDate BETWEEN :start AND :end")
    List<StudyDayOwner> findDatesForUsers(@Param("userIds") Collection<Long> userIds,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);
}
