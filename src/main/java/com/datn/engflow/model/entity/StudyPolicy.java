package com.datn.engflow.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/** Mốc áp dụng được khởi tạo một lần bởi SQL triển khai, không bởi mỗi lần boot. */
@Entity
@Table(name = "study_policy")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPolicy {
    @Id
    private Integer id;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDate effectiveFrom;
}
