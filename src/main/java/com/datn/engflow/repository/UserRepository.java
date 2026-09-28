package com.datn.engflow.repository;

import com.datn.engflow.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng {@code users} — tài khoản, hồ sơ, trạng thái premium và streak.
 *
 * <p>Được dùng rộng khắp service; các truy vấn nhạy cảm về khoá ở đây:
 * {@link #findForStudyUpdate} khoá bi quan để cập nhật streak an toàn, và
 * {@link #updateExpiredPremium} là bulk update chạy theo lịch.
 *
 * <p>Từ audit-v13 F-13-08, nguồn sự thật của lịch học là bảng {@code study_days}
 * chứ không còn là cột {@code users.last_study_date} (đã bị xoá).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Đọc user kèm khoá ghi bi quan ({@code PESSIMISTIC_WRITE}) — dùng khi vừa đọc vừa
     * sửa streak để hai request đồng thời không ghi đè nhau.
     *
     * <p>Phải gọi trong transaction, nếu không khoá không được giữ tới lúc ghi.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findForStudyUpdate(@Param("id") Long id);

    /**
     * User đang hoạt động mà ngày học gần nhất trong khoảng [{@code start}, {@code today}]
     * đúng bằng {@code yesterday} — tức hôm qua có học, hôm nay chưa: nguy cơ mất streak.
     * Dùng cho email nhắc nhở.
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND "
            + "(SELECT MAX(d.studyDate) FROM StudyDay d WHERE d.userId = u.id "
            + "AND d.studyDate BETWEEN :start AND :today) = :yesterday")
    List<User> findStudyReminderAtRisk(@Param("start") LocalDate start,
            @Param("today") LocalDate today, @Param("yesterday") LocalDate yesterday);

    /**
     * User đang hoạt động mà ngày học gần nhất trong khoảng [{@code start}, {@code today}]
     * cũ hơn {@code yesterday} — tức streak đã đứt. Dùng cho email "bắt đầu lại".
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND "
            + "(SELECT MAX(d.studyDate) FROM StudyDay d WHERE d.userId = u.id "
            + "AND d.studyDate BETWEEN :start AND :today) < :yesterday")
    List<User> findStudyReminderBroken(@Param("start") LocalDate start,
            @Param("today") LocalDate today, @Param("yesterday") LocalDate yesterday);

    /** Tra user theo email — định danh đăng nhập; được JWT filter và service dùng liên tục. */
    Optional<User> findByEmail(String email);
    /** Tra user theo username — dùng cho đăng nhập/kiểm tra trùng. */
    Optional<User> findByUsername(String username);
    /** Email đã tồn tại chưa — kiểm tra khi đăng ký. */
    boolean existsByEmail(String email);
    /** Username đã tồn tại chưa — kiểm tra khi đăng ký. */
    boolean existsByUsername(String username);

    /**
     * Tìm user theo username/email/fullName chứa từ khóa, không phân trang — giữ cho
     * đường cũ. Bản {@link #searchByKeywordPage} mới là đường dùng thật ở màn admin.
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    /** Bản phân trang của {@link #searchByKeyword(String)} — danh sách user ở màn admin. */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchByKeywordPage(@Param("keyword") String keyword, Pageable pageable);

    /** Đếm user đang hoạt động — cho thống kê dashboard. */
    long countByIsActiveTrue();

    // audit-v13 F-13-08: bốn truy vấn trên cột legacy `users.last_study_date` đã bị XOÁ
    // tại đây (countByLastStudyDateAfter, findUsersWhoHaveNotLoggedInSince,
    // findUsersWithBrokenStreak, findActiveUsersWhoLastStudiedOn). Cả bốn đều 0 caller;
    // cột này cũng bị drop trong cùng thay đổi. Đường nhắc nhở thay thế chúng truy vấn
    // bảng `study_days` — xem findStudyReminderAtRisk / findStudyReminderBroken ở trên.

    /**
     * Bulk hạ premium đã hết hạn về {@code false} và xoá {@code premiumExpiry}; chạy theo
     * lịch ({@code PremiumExpiryScheduler}).
     *
     * <p>Điều kiện {@code premiumExpiry IS NOT NULL} là cố ý: tài khoản premium vĩnh viễn
     * (không có ngày hết hạn) không được đụng tới.
     *
     * @return số dòng đã cập nhật
     */
    @Modifying
    @Query("UPDATE User u SET u.isPremium = false, u.premiumExpiry = NULL WHERE u.isPremium = true AND u.premiumExpiry IS NOT NULL AND u.premiumExpiry < :today")
    int updateExpiredPremium(@Param("today") LocalDate today);
}
