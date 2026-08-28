package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deck_words", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"deck_id", "vocab_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
/**
 * class DeckWord.
 */
public class DeckWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deck_word_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Deck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocabulary;

    @Column(name = "order_index")
    private Integer orderIndex;
}
