package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.DeckRequest;
import com.datn.engflow.model.dto.response.DeckSummaryResponse;
import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckRepository;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * Quản lý bộ từ (deck) của người học: đọc công khai/cá nhân, tạo/sửa/xóa và thêm từ vào bộ.
 *
 * <p>Tầng service, được gọi từ {@code DeckController} và từ {@link SrsService},
 * {@link VocabularyService}. Truy cập dữ liệu qua {@link DeckRepository},
 * {@link DeckWordRepository}, {@link UserRepository}, {@link VocabularyRepository}.
 *
 * <p>Quy tắc sở hữu: deck public đọc được cho mọi người, deck private chỉ chủ sở hữu;
 * mọi thao tác ghi đều kiểm tra {@code owner_id} trước khi thực hiện.
 */
public class DeckService {

    private final DeckRepository deckRepository;
    private final DeckWordRepository deckWordRepository;
    private final UserRepository userRepository;
    private final VocabularyRepository vocabularyRepository;

    /**
     * Bản không lọc của {@link #getAllPublicDecks(String)}.
     *
     * @return mọi deck public
     */
    public List<Deck> getAllPublicDecks() {
        return getAllPublicDecks(null);
    }

    /**
     * Danh sách deck public, tùy chọn lọc theo từ khóa.
     *
     * @param keyword từ khóa tìm trong tên/mô tả; null/rỗng là lấy tất cả
     * @return danh sách deck public
     */
    public List<Deck> getAllPublicDecks(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return deckRepository.searchPublic(keyword.trim());
        }
        return deckRepository.findByIsPublicTrue();
    }

    /**
     * Bản không lọc của {@link #getUserDecks(Long, String)}.
     *
     * @param userId id chủ sở hữu
     * @return mọi deck của user
     */
    public List<Deck> getUserDecks(Long userId) {
        return getUserDecks(userId, null);
    }

    /**
     * Danh sách deck của một user, tùy chọn lọc theo từ khóa.
     *
     * @param userId id chủ sở hữu
     * @param keyword từ khóa tìm trong tên/mô tả; null/rỗng là lấy tất cả
     * @return danh sách deck thuộc user
     */
    public List<Deck> getUserDecks(Long userId, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return deckRepository.searchByOwner(userId, keyword.trim());
        }
        return deckRepository.findByOwnerId(userId);
    }

    /**
     * Trang deck public kèm số từ của mỗi deck.
     *
     * @param keyword từ khóa tìm kiếm; null/rỗng là không lọc
     * @param pageable thông số phân trang
     * @return trang tóm tắt deck đã gắn wordCount
     */
    public Page<DeckSummaryResponse> getPublicDeckPage(String keyword, Pageable pageable) {
        Page<Deck> page = deckRepository.findPublicPage(normalizeKeyword(keyword), pageable);
        Page<DeckSummaryResponse> summary = page.map(this::toSummary);
        attachWordCounts(summary);
        return summary;
    }

    /**
     * Trang deck của một user kèm số từ của mỗi deck.
     *
     * @param userId id chủ sở hữu
     * @param keyword từ khóa tìm kiếm; null/rỗng là không lọc
     * @param pageable thông số phân trang
     * @return trang tóm tắt deck đã gắn wordCount
     */
    public Page<DeckSummaryResponse> getUserDeckPage(Long userId, String keyword, Pageable pageable) {
        Page<Deck> page = deckRepository.findOwnerPage(userId, normalizeKeyword(keyword), pageable);
        Page<DeckSummaryResponse> summary = page.map(this::toSummary);
        attachWordCounts(summary);
        return summary;
    }

    /** Chuẩn hóa từ khóa: trim, coi chuỗi rỗng/blank như null để bỏ điều kiện lọc. */
    private String normalizeKeyword(String keyword) {
        return keyword != null && !keyword.isBlank() ? keyword.trim() : null;
    }

    /** Map entity Deck sang DTO tóm tắt; wordCount được điền sau bởi attachWordCounts. */
    private DeckSummaryResponse toSummary(Deck deck) {
        return DeckSummaryResponse.builder()
                .id(deck.getId())
                .name(deck.getName())
                .description(deck.getDescription())
                .source(deck.getSource())
                .cefrLevel(deck.getCefrLevel())
                .isPublic(deck.getIsPublic())
                .thumbnailUrl(deck.getThumbnailUrl())
                .wordCount(null)
                .createdAt(deck.getCreatedAt())
                .updatedAt(deck.getUpdatedAt())
                .build();
    }

    /**
     * Điền wordCount cho cả trang bằng MỘT query gộp theo danh sách deck id (tránh N+1).
     *
     * @param page trang DTO cần gắn số từ, sửa tại chỗ
     */
    private void attachWordCounts(Page<DeckSummaryResponse> page) {
        List<Long> deckIds = page.getContent().stream().map(DeckSummaryResponse::getId).toList();
        if (deckIds.isEmpty()) return;
        Map<Long, Long> counts = deckRepository.countWordsByDeckIds(deckIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        page.getContent().forEach(dto -> dto.setWordCount(counts.getOrDefault(dto.getId(), 0L).intValue()));
    }

    /**
     * Đọc một deck nếu người gọi có quyền xem.
     *
     * @param deckId id deck
     * @param userId id người gọi; null (khách) chỉ xem được deck public
     * @return entity deck
     * @throws ResourceNotFoundException nếu deck không tồn tại
     * @throws BadRequestException nếu deck private và người gọi không phải chủ sở hữu
     */
    public Deck getDeckById(Long deckId, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
        if (!deck.getIsPublic() && (userId == null || !userId.equals(deck.getOwner().getId()))) {
            throw new BadRequestException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n truy c\u1eadp b\u1ed9 t\u1eeb v\u1ef1ng n\u00e0y");
        }
        return deck;
    }

    /**
     * Tạo deck mới thuộc sở hữu của user. Mặc định {@code isPublic=true} khi request bỏ trống.
     *
     * @param request dữ liệu deck
     * @param userId id chủ sở hữu
     * @return entity deck đã lưu
     */
    @Transactional
    public Deck createDeck(DeckRequest request, Long userId) {
        User owner = userRepository.findById(userId).orElseThrow();
        Deck deck = Deck.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .source(request.getSource())
                .cefrLevel(request.getCefrLevel())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .thumbnailUrl(request.getThumbnailUrl())
                .build();
        return deckRepository.save(deck);
    }

    /**
     * Cập nhật deck; chỉ chủ sở hữu được sửa. {@code isPublic} null sẽ giữ nguyên giá trị cũ.
     *
     * @param deckId id deck cần sửa
     * @param request dữ liệu mới
     * @param userId id người gọi, phải là chủ sở hữu
     * @return entity deck đã lưu
     * @throws ResourceNotFoundException nếu deck không tồn tại
     * @throws BadRequestException nếu người gọi không phải chủ sở hữu
     */
    @Transactional
    public Deck updateDeck(Long deckId, DeckRequest request, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
        if (deck.getOwner() == null || !deck.getOwner().getId().equals(userId)) {
            throw new BadRequestException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n s\u1eeda b\u1ed9 t\u1eeb n\u00e0y");
        }
        deck.setName(request.getName());
        deck.setDescription(request.getDescription());
        deck.setSource(request.getSource());
        deck.setCefrLevel(request.getCefrLevel());
        if (request.getIsPublic() != null) deck.setIsPublic(request.getIsPublic());
        deck.setThumbnailUrl(request.getThumbnailUrl());
        return deckRepository.save(deck);
    }

    /**
     * Xóa deck; chỉ chủ sở hữu được xóa.
     *
     * @param deckId id deck cần xóa
     * @param userId id người gọi, phải là chủ sở hữu
     * @throws ResourceNotFoundException nếu deck không tồn tại
     * @throws BadRequestException nếu người gọi không phải chủ sở hữu
     */
    @Transactional
    public void deleteDeck(Long deckId, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
        if (deck.getOwner() == null || !deck.getOwner().getId().equals(userId)) {
            throw new BadRequestException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n x\u00f3a b\u1ed9 t\u1eeb n\u00e0y");
        }
        deckRepository.delete(deck);
    }

    /**
     * Thêm một từ vựng vào deck. Idempotent: nếu từ đã có trong deck thì không làm gì.
     *
     * <p>Thứ tự {@code orderIndex} được nối tiếp sau từ cuối cùng hiện có (bắt đầu từ 1).
     * {@link VocabularyService#createScoped} dùng chung transaction với method này nên nếu
     * bước kiểm tra quyền thất bại, từ vựng vừa tạo cũng bị rollback theo.
     *
     * @param deckId id deck đích
     * @param vocabId id từ vựng cần thêm
     * @param userId id người gọi, phải là chủ sở hữu deck
     * @throws ResourceNotFoundException nếu deck hoặc từ vựng không tồn tại
     * @throws BadRequestException nếu người gọi không phải chủ sở hữu deck
     */
    @Transactional
    public void addWordToDeck(Long deckId, Long vocabId, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
        if (deck.getOwner() == null || !deck.getOwner().getId().equals(userId)) {
            throw new BadRequestException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n ch\u1ec9nh s\u1eeda b\u1ed9 t\u1eeb n\u00e0y");
        }
        Vocabulary vocab = vocabularyRepository.findById(vocabId)
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary", "id", vocabId));

        if (deckWordRepository.existsByDeckIdAndVocabularyId(deckId, vocabId)) {
            return;
        }

        List<DeckWord> existingWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
        int nextOrder = existingWords.isEmpty() ? 1 : existingWords.get(existingWords.size() - 1).getOrderIndex() + 1;

        DeckWord deckWord = DeckWord.builder()
                .deck(deck)
                .vocabulary(vocab)
                .orderIndex(nextOrder)
                .build();

        deckWordRepository.save(deckWord);
    }
}
