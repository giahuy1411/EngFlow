package com.datn.engflow.controller;

import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.VocabularyRepository;
import com.datn.engflow.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import com.datn.engflow.service.DictionaryService;
import com.datn.engflow.service.VocabularyService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vocabulary")
@RequiredArgsConstructor
/**
 * class VocabularyController.
 */
public class VocabularyController {

    private final VocabularyRepository vocabularyRepository;
    private final DictionaryService dictionaryService;
    private final VocabularyService vocabularyService;

    @GetMapping
    public ResponseEntity<Page<Vocabulary>> list(@PageableDefault(size = 20, sort = "word") Pageable pageable) {
        return ResponseEntity.ok(vocabularyRepository.findAll(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Vocabulary>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String q) {
        String query = keyword.isBlank() ? q : keyword;
        if (query.isBlank() || query.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        List<Vocabulary> results = vocabularyRepository.findByWordContainingIgnoreCase(query);
        return ResponseEntity.ok(results);
    }

    /**
     * Proxy tra từ điển dictionaryapi.dev — browser ở VN đôi khi không kết nối
     * trực tiếp được tới API này, trong khi backend container thì được.
     * Cache + timeout nằm ở DictionaryService (bắt buộc tách class để
     * @Cacheable đi qua Spring proxy).
     * Fail-soft: lỗi upstream → "[]".
     */
    @GetMapping("/dictionary/{word}")
    public ResponseEntity<String> dictionaryProxy(@PathVariable String word) {
        String clean = word.replaceAll("[^a-zA-Z'-]", "").toLowerCase();
        if (clean.isBlank()) {
            return ResponseEntity.badRequest().body("[]");
        }
        return ResponseEntity.ok(dictionaryService.lookup(clean));
    }

    @PostMapping
    public ResponseEntity<Vocabulary> create(
            @Valid @RequestBody VocabularyRequest request,
            @RequestParam(name = "deckId", required = false) Long deckId,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // audit-v12 F147: a non-admin must name the deck the word goes into, and the link is
        // made server-side in the same transaction. Previously the client had to make a
        // second call to link the deck, which could fail and strand the word in the shared
        // dictionary, and nothing checked that the deck belonged to the caller.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        Long userId = (authentication.getPrincipal() instanceof UserPrincipal principal)
                ? principal.getId()
                : null;
        Vocabulary saved = vocabularyService.createScoped(request, deckId, userId, isAdmin);
        return ResponseEntity.ok(saved);
    }
}
