package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Ngày hoàn thành hoạt động học; không đại diện cho ngày đăng nhập.
 *
 * <p>Tầng entity. Đây là nguồn sự thật duy nhất cho streak: {@link StreakService}
 * và {@link StudyActivityService} đọc/ghi bảng này, không đọc cột ngày nào trên
 * {@link User}. Cặp {@code (user_id, study_date)} là duy nhất và ngày lấy theo múi
 * giờ {@code Asia/Ho_Chi_Minh} của {@link StudyActivityService}, nên một ngày
 * học chỉ được ghi một lần bất kể người học làm mấy bài.
 */
@Entity
@Table(name = "study_days", uniqueConstraints = @UniqueConstraint(
        name = "uq_study_days_user_date", columnNames = {"user_id", "study_date"}))
@Getter
@NoArgsConstructor
public class StudyDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    /**
     * Tạo một dòng ngày học mới.
     *
     * <p>Không có builder: {@link StudyActivityService} chỉ dựng dòng này sau khi
     * đã kiểm tra ngày đó chưa tồn tại cho user, nên không có chỗ nào cần sửa các
     * trường sau khi tạo.
     *
     * @param userId id của người học, không null
     * @param studyDate ngày học theo múi giờ học tập, không null
     */
    public StudyDay(Long userId, LocalDate studyDate) {
        this.userId = userId;
        this.studyDate = studyDate;
    }
}
