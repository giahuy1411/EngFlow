package com.datn.engflow.repository;

import com.datn.engflow.model.entity.DeckWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng nối {@code deck_words} — thứ tự từ vựng bên trong một bộ từ.
 *
 * <p>Dùng bởi {@code DeckService} (dựng/lọc danh sách từ), {@code SrsService}
 * (lấy tập từ để tính lịch ôn) và {@code GameService} (rút ngẫu nhiên câu hỏi).
 * Mỗi bản ghi giữ {@code orderIndex} để giữ đúng thứ tự người dùng đã sắp.
 */
@Repository
public interface DeckWordRepository extends JpaRepository<DeckWord, Long> {
    /**
     * Các từ của một bộ theo thứ tự đã sắp, tải kèm {@code vocabulary} trong cùng
     * một truy vấn để không bị N+1 khi dựng response chi tiết bộ từ.
     *
     * @param deckId id bộ từ
     * @return danh sách từ của bộ, tăng dần {@code orderIndex}
     */
    @Query("SELECT dw FROM DeckWord dw JOIN FETCH dw.vocabulary WHERE dw.deck.id = :deckId ORDER BY dw.orderIndex ASC")
    List<DeckWord> findByDeckIdOrderByOrderIndexAsc(@Param("deckId") Long deckId);

    /**
     * Tìm bản ghi nối của một từ trong một bộ.
     *
     * @param deckId   id bộ từ
     * @param vocabId  id từ vựng
     * @return bản ghi nối nếu từ đã nằm trong bộ
     */
    Optional<DeckWord> findByDeckIdAndVocabularyId(Long deckId, Long vocabId);

    /**
     * Kiểm tra từ đã nằm trong bộ chưa, dùng để chặn thêm trùng.
     *
     * @param deckId  id bộ từ
     * @param vocabId id từ vựng
     * @return true nếu bộ đã chứa từ này
     */
    boolean existsByDeckIdAndVocabularyId(Long deckId, Long vocabId);
}
