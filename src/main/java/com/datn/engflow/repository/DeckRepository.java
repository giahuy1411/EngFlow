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

@Repository
/**
 * interface DeckRepository.
 */
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByIsPublicTrue();
    List<Deck> findByOwnerId(Long ownerId);
    List<Deck> findByIsPublicTrueOrOwnerId(Long ownerId);
    List<Deck> findBySource(String source);

    @Query("SELECT d FROM Deck d WHERE d.isPublic = true AND "
           + "(LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
           + "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Deck> searchPublic(@Param("keyword") String keyword);

    @Query("SELECT d FROM Deck d WHERE d.owner.id = :ownerId AND "
           + "(LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
           + "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Deck> searchByOwner(@Param("ownerId") Long ownerId, @Param("keyword") String keyword);

    Page<Deck> findByIsPublicTrue(Pageable pageable);

    Page<Deck> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT d FROM Deck d WHERE d.isPublic = true AND "
           + "(:keyword IS NULL OR :keyword = '' OR "
           + "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
           + "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Deck> findPublicPage(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Deck d WHERE d.owner.id = :ownerId AND "
           + "(:keyword IS NULL OR :keyword = '' OR "
           + "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
           + "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Deck> findOwnerPage(@Param("ownerId") Long ownerId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT dw.deck.id, COUNT(dw) FROM DeckWord dw WHERE dw.deck.id IN :deckIds GROUP BY dw.deck.id")
    List<Object[]> countWordsByDeckIds(@Param("deckIds") Collection<Long> deckIds);
}
