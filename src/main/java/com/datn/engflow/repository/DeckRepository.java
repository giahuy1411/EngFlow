package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Truy cập bảng {@code decks} — bộ từ vựng do người dùng tự tạo hoặc seed sẵn.
 *
 * <p>Đọc bởi {@code DeckService} (duyệt, tìm kiếm, phân trang, đếm số từ) và
 * {@code GameService}; {@code VocabularyDataSeeder} dùng khi nạp dữ liệu khởi
 * tạo. Các truy vấn tìm kiếm đều gộp cả {@code name} lẫn {@code description},
 * bọc {@code LOWER} hai phía để không phụ thuộc hoa thường.
 */
@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    /**
     * Toàn bộ bộ từ vựng công khai.
     *
     * @return danh sách deck có {@code isPublic = true}
     */
    List<Deck> findByIsPublicTrue();

    /**
     * Các bộ từ vựng thuộc sở hữu của một người dùng.
     *
     * @param ownerId id chủ sở hữu
     * @return danh sách deck của người dùng đó
     */
    List<Deck> findByOwnerId(Long ownerId);

    /**
     * Bộ từ vựng công khai và bộ của chủ nhân, gộp lại cho danh sách "deck của tôi".
     *
     * @param ownerId id chủ sở hữu
     * @return danh sách deck công khai cộng deck riêng của người dùng
     */
    List<Deck> findByIsPublicTrueOrOwnerId(Long ownerId);

    /**
     * Deck theo nguồn sinh ra, ví dụ seed hay do AI tạo.
     *
     * @param source mã nguồn cần lọc
     * @return danh sách deck có {@code source} khớp
     */
    List<Deck> findBySource(String source);

    /**
     * Tìm kiếm trong các deck công khai theo tên hoặc mô tả.
     *
     * @param keyword từ khóa thô, đã được {@code DeckService} trim trước khi gọi
     * @return danh sách deck công khai khớp từ khóa
     */
    @Query("SELECT d FROM Deck d WHERE d.isPublic = true AND "
           + "(LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
           + "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Deck> searchPublic(@Param("keyword") String keyword);

    /**
     * Tìm kiếm trong bộ từ của riêng một người dùng; tách khỏi {@link #searchPublic}
     * để không bao giờ lọc nhầm sang deck của người khác.
     *
     * @param ownerId id chủ sở hữu
     * @param keyword từ khóa thô, đã được {@code DeckService} trim trước khi gọi
     * @return danh sách deck của người dùng đó khớp từ khóa
     */
    @Query("SELECT d FROM Deck d WHERE d.owner.id = :ownerId AND "
           + "(LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
           + "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Deck> searchByOwner(@Param("ownerId") Long ownerId, @Param("keyword") String keyword);

    /**
     * Trang đầu tiên của các deck công khai, chưa lọc từ khóa.
     *
     * @param pageable cấu hình phân trang do controller dựng từ query param
     * @return trang các deck công khai
     */
    Page<Deck> findByIsPublicTrue(Pageable pageable);

    /**
     * Trang các deck thuộc sở hữu của một người dùng, chưa lọc từ khóa.
     *
     * @param ownerId id chủ sở hữu
     * @param pageable cấu hình phân trang
     * @return trang deck của người dùng đó
     */
    Page<Deck> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Trang deck công khai kèm tìm kiếm. Khoảng rỗng được xử lý ngay trong JPQL
     * nên controller không cần dựng truy vấn khác khi không có từ khóa.
     *
     * @param keyword từ khóa, null hoặc rỗng thì trả toàn bộ
     * @param pageable cấu hình phân trang
     * @return trang deck công khai khớp điều kiện lọc
     */
    @Query("SELECT d FROM Deck d WHERE d.isPublic = true AND "
           + "(:keyword IS NULL OR :keyword = '' OR "
           + "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
           + "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Deck> findPublicPage(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Trang deck riêng của một người dùng kèm tìm kiếm.
     *
     * @param ownerId id chủ sở hữu
     * @param keyword từ khóa, null hoặc rỗng thì trả toàn bộ
     * @param pageable cấu hình phân trang
     * @return trang deck của người dùng đó khớp điều kiện lọc
     */
    @Query("SELECT d FROM Deck d WHERE d.owner.id = :ownerId AND "
           + "(:keyword IS NULL OR :keyword = '' OR "
           + "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
           + "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Deck> findOwnerPage(@Param("ownerId") Long ownerId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * Đếm số từ của nhiều deck trong một lượt truy vấn, dùng để gắn số từ lên
     * từng dòng của danh sách thay vì gọi đếm riêng từng deck.
     *
     * <p>Chỉ deck có từ mới xuất hiện trong kết quả — deck rỗng không có dòng nào
     * và {@code DeckService} tự mặc định cho màu 0.
     *
     * @param deckIds danh sách id deck cần đếm
     * @return danh sách cặp {@code [deckId, soLuongTu]}
     */
    @Query("SELECT dw.deck.id, COUNT(dw) FROM DeckWord dw WHERE dw.deck.id IN :deckIds GROUP BY dw.deck.id")
    List<Object[]> countWordsByDeckIds(@Param("deckIds") Collection<Long> deckIds);
}
