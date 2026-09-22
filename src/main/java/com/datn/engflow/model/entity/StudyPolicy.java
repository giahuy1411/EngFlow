package com.datn.engflow.model.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Mốc áp dụng được khởi tạo một lần bởi SQL triển khai, không bởi mỗi lần boot.
 *
 * <p>audit-v13 F-13-07: the singleton CHECK was missing in the live DB because
 * {@code tasks/streak-study/deploy.sql} declared it inside the table-existence guard,
 * and Hibernate creates the table first, so the guard was skipped — the same bug class
 * as audit-v10 F124. Declared here with JPA 3.2 {@code @CheckConstraint} so a fresh
 * schema always carries it. NOTE: {@code ddl-auto=update} does NOT add a check
 * constraint to an ALREADY EXISTING table, so the live DB is patched by
 * {@code tasks/streak-study/fix-study-policy-check.sql} as well.
 */
@Entity
@Table(
        name = "study_policy",
        check = @CheckConstraint(
                name = "ck_study_policy_singleton",
                constraint = "id = 1"
        )
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPolicy {
    @Id
    private Integer id;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDate effectiveFrom;
}
