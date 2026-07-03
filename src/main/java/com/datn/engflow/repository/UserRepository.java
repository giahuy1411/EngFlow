package com.datn.engflow.repository;

import com.datn.engflow.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // Get active users who have not logged in within the given time threshold
    @Query("SELECT u FROM User u WHERE u.isActive = true AND (u.lastStudyDate IS NULL OR u.lastStudyDate < :threshold)")
    List<User> findUsersWhoHaveNotLoggedInSince(@Param("threshold") LocalDate threshold);
}
