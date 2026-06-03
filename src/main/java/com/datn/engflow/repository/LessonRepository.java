package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.LessonLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByIsPublishedTrueOrderByOrderIndexAsc();
    List<Lesson> findByLevelAndIsPublishedTrue(LessonLevel level);
    long countByIsPublishedTrue();

    @Query("SELECT DISTINCT l FROM Lesson l " +
           "LEFT JOIN FETCH l.vocabularies " +
           "WHERE l.id = :id")
    Optional<Lesson> findByIdWithDetails(@Param("id") Long id);
}
