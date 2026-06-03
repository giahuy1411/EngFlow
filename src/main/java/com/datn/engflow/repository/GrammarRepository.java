package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Grammar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrammarRepository extends JpaRepository<Grammar, Long> {
    List<Grammar> findByLessonId(Long lessonId);
}
