package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByIsPublicTrue();
    List<Deck> findByOwnerId(Long ownerId);
    List<Deck> findByIsPublicTrueOrOwnerId(Long ownerId);
    List<Deck> findBySource(String source);
}
