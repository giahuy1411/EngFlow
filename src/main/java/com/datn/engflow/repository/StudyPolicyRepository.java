package com.datn.engflow.repository;

import com.datn.engflow.model.entity.StudyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

/** Đọc mốc chuyển đổi đã chốt, không tự tạo lại khi restart. */
public interface StudyPolicyRepository extends JpaRepository<StudyPolicy, Integer> {
}
