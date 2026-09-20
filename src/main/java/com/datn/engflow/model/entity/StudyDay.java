package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/** Ngày hoàn thành hoạt động học; không đại diện cho ngày đăng nhập. */
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

    public StudyDay(Long userId, LocalDate studyDate) {
        this.userId = userId;
        this.studyDate = studyDate;
    }
}
