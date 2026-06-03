package com.datn.engflow.config;

import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.model.enums.DeckSource;
import com.datn.engflow.repository.DeckRepository;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class VocabularyDataSeeder implements CommandLineRunner {

    private final VocabularyRepository vocabularyRepository;
    private final DeckRepository deckRepository;
    private final DeckWordRepository deckWordRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (deckRepository.count() == 0) {
            log.info("Seeding static vocabulary decks...");
            seedOxford3000();
            seedAWL();
            seedTOEIC();
            seedIELTS();
            seedTHPT();
            log.info("Vocabulary seeding completed.");
        }
    }

    private void seedOxford3000() {
        Deck deck = createDeck("Oxford 3000 (A1-B2)", "The 3000 most important words to learn in English.", DeckSource.OXFORD3000, "B2");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("ambitious", "/æmˈbɪʃ.əs/", "có tham vọng", "Having a strong desire for success.", "She is ambitious and wants to become a doctor.", "adjective", "B2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("benefit", "/ˈben.ɪ.fɪt/", "lợi ích", "A helpful or good effect.", "The discovery of oil brought many benefits to the town.", "noun", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("candidate", "/ˈkæn.dɪ.dət/", "ứng cử viên", "A person who is competing to get a job or elected position.", "There are three candidates standing in the election.", "noun", "B1", DeckSource.OXFORD3000));
        vocabs.add(createVocab("determine", "/dɪˈtɜː.mɪn/", "xác định", "To control or influence something directly.", "Your health is determined in part by what you eat.", "verb", "B1", DeckSource.OXFORD3000));
        vocabs.add(createVocab("essential", "/ɪˈsen.ʃəl/", "thiết yếu", "Necessary or needed.", "Water is essential for living things.", "adjective", "B1", DeckSource.OXFORD3000));
        saveDeckWords(deck, vocabs);
    }

    private void seedAWL() {
        Deck deck = createDeck("Academic Word List", "The most frequent academic words.", DeckSource.AWL, "C1");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("analyze", "/ˈæn.əl.aɪz/", "phân tích", "To study or examine something in detail.", "Researchers analyzed the purchases of 6,300 households.", "verb", "B2", DeckSource.AWL));
        vocabs.add(createVocab("concept", "/ˈkɒn.sept/", "khái niệm", "A principle or idea.", "The concept of free speech is unknown to them.", "noun", "B2", DeckSource.AWL));
        vocabs.add(createVocab("evident", "/ˈev.ɪ.dənt/", "hiển nhiên", "Easily seen or understood.", "The full extent of the damage only became evident the following morning.", "adjective", "B2", DeckSource.AWL));
        vocabs.add(createVocab("method", "/ˈmeθ.əd/", "phương pháp", "A particular way of doing something.", "Travelling by train is still one of the safest methods of transport.", "noun", "A2", DeckSource.AWL));
        saveDeckWords(deck, vocabs);
    }

    private void seedTOEIC() {
        Deck deck = createDeck("TOEIC 600 Essential Words", "Vocabulary for the TOEIC test, focusing on business contexts.", DeckSource.TOEIC, "B2");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("contract", "/ˈkɒn.trækt/", "hợp đồng", "A legal document that states and explains a formal agreement.", "They could take legal action against you if you break the contract.", "noun", "B1", DeckSource.TOEIC));
        vocabs.add(createVocab("negotiate", "/nəˈɡəʊ.ʃi.eɪt/", "đàm phán", "To have formal discussions with someone in order to reach an agreement.", "The government has refused to negotiate with the strikers.", "verb", "C1", DeckSource.TOEIC));
        vocabs.add(createVocab("revenue", "/ˈrev.ən.juː/", "doanh thu", "The income that a government or company receives regularly.", "Taxes provide most of the government's revenue.", "noun", "C1", DeckSource.TOEIC));
        saveDeckWords(deck, vocabs);
    }

    private void seedIELTS() {
        Deck deck = createDeck("IELTS Academic: Environment", "Vocabulary related to climate change, pollution, and the environment.", DeckSource.IELTS, "C1");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("mitigate", "/ˈmɪt.ɪ.ɡeɪt/", "giảm nhẹ", "To make something less severe, harmful, or painful.", "It is unclear how to mitigate the effects of tourism on the island.", "verb", "C1", DeckSource.IELTS));
        vocabs.add(createVocab("sustainable", "/səˈsteɪ.nə.bəl/", "bền vững", "Causing little or no damage to the environment.", "A large international meeting was held with the aim of promoting sustainable development.", "adjective", "C1", DeckSource.IELTS));
        saveDeckWords(deck, vocabs);
    }

    private void seedTHPT() {
        Deck deck = createDeck("THPT Quốc Gia: Lớp 12", "Từ vựng sách giáo khoa lớp 12 chuẩn bị cho kỳ thi THPT QG.", DeckSource.THPT, "B1");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("urbanization", "/ˌɜː.bən.aɪˈzeɪ.ʃən/", "đô thị hóa", "The process by which more and more people leave the countryside to live in cities.", "Fast urbanization can cause severe environmental problems.", "noun", "B2", DeckSource.THPT));
        vocabs.add(createVocab("biodiversity", "/ˌbaɪ.əʊ.daɪˈvɜː.sə.ti/", "đa dạng sinh học", "The number and types of plants and animals that exist in a particular area.", "A new National Biological Survey will protect species habitat and biodiversity.", "noun", "C1", DeckSource.THPT));
        saveDeckWords(deck, vocabs);
    }

    private Deck createDeck(String name, String description, DeckSource source, String cefrLevel) {
        Deck deck = Deck.builder()
                .name(name)
                .description(description)
                .source(source.name())
                .cefrLevel(cefrLevel)
                .isPublic(true)
                .build();
        return deckRepository.save(deck);
    }

    private Vocabulary createVocab(String word, String ipa, String vi, String en, String example, String pos, String level, DeckSource source) {
        return Vocabulary.builder()
                .word(word)
                .pronunciation(ipa)
                .meaning(vi)
                .definitionEn(en)
                .exampleSentence(example)
                .wordType(pos)
                .cefrLevel(level)
                .source(source.name())
                .build();
    }

    private void saveDeckWords(Deck deck, List<Vocabulary> vocabs) {
        vocabularyRepository.saveAll(vocabs);
        int index = 1;
        List<DeckWord> deckWords = new ArrayList<>();
        for (Vocabulary v : vocabs) {
            deckWords.add(DeckWord.builder()
                    .deck(deck)
                    .vocabulary(v)
                    .orderIndex(index++)
                    .build());
        }
        deckWordRepository.saveAll(deckWords);
    }
}
