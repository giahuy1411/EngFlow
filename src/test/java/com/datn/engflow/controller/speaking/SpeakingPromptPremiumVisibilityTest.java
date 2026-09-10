package com.datn.engflow.controller.speaking;

import com.datn.engflow.model.entity.SpeakingPrompt;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.SpeakingPromptRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prompt đã publish nhưng đánh dấu premium không được rơi vào các endpoint công khai
 * ("/api/v1/speaking-prompts", "/api/v1/video-prompts" và bản /{id}) cho người không
 * có quyền — hai đường này permitAll nên ai gọi thẳng API cũng đọc được nội dung,
 * bỏ qua đúng điều "Premium" mà admin tick trong bảng đề nói.
 *
 * <p>Trang UI /speaking vốn đã chặn bằng router guard nên triệu chứng không lộ ra
 * giao diện; đây là chốt ở tầng server, tầng duy nhất đáng tin. Quyền premium lấy
 * từ {@code UserService.hasPremiumAccess} (đọc DB) nên tài khoản hết hạn gói vẫn bị
 * ẩn, không phụ thuộc flag gắn trong JWT.</p>
 */
@SpringBootTest
@Transactional
class SpeakingPromptPremiumVisibilityTest {

    private static final String FREE_TITLE = "ZZTEST free speaking prompt";
    private static final String PREMIUM_TITLE = "ZZTEST premium speaking prompt";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SpeakingPromptRepository repository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private Long premiumId;

    @BeforeEach
    void setup() {
        // MockMvc o day khong gan security filter chain nen SecurityContext phai do
        // chinh test quan ly (loat theo GameControllerTest trong repo).
        SecurityContextHolder.clearContext();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Lop @Transactional rollback sau moi test — khong can don du lieu thu nghiem.
        repository.save(SpeakingPrompt.builder()
                .title(FREE_TITLE).prompt("Speak about your weekend.").isPremium(false).isPublished(true)
                .orderIndex(900).build());
        premiumId = repository.save(SpeakingPrompt.builder()
                .title(PREMIUM_TITLE).prompt("Secret premium task text that must not leak.").isPremium(true).isPublished(true)
                .orderIndex(901).build()).getId();
    }

    /** Dang nhap bang tai khoan that trong DB de entitlement doc duoc tu UserService. */
    private RequestPostProcessor authAs(String username, User user) {
        User saved = userRepository.save(User.builder()
                .username(username)
                .email(username + "@test.com")
                .passwordHash("not-a-real-hash")
                .isPremium(user.getIsPremium())
                .premiumExpiry(user.getPremiumExpiry())
                .isAdmin(user.getIsAdmin())
                .build());
        UserPrincipal principal = new UserPrincipal(saved);
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            return request;
        };
    }

    private User user(boolean premium, LocalDate expiry) {
        return User.builder().isPremium(premium).premiumExpiry(expiry).isAdmin(false).build();
    }

    private User adminUser() {
        return User.builder().isPremium(false).isAdmin(true).build();
    }

    private void listExcludes(String title) throws Exception {
        mockMvc.perform(get("/api/v1/speaking-prompts").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", not(hasItem(title))));
    }

    @Test
    void anonymousList_hidesPremiumPrompt_butKeepsFreePrompt() throws Exception {
        listExcludes(PREMIUM_TITLE);
        mockMvc.perform(get("/api/v1/speaking-prompts").param("size", "50"))
                .andExpect(jsonPath("$.content[*].title", hasItem(FREE_TITLE)));
    }

    @Test
    void freeUserList_hidesPremiumPrompt() throws Exception {
        mockMvc.perform(get("/api/v1/speaking-prompts").param("size", "50")
                        .with(authAs("zzfree", user(false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", not(hasItem(PREMIUM_TITLE))))
                .andExpect(jsonPath("$.content[*].title", hasItem(FREE_TITLE)));
    }

    /** Het han goi bi an nhu tai khoan mien phi — nguon chan ly la DB, khong phai flag trong JWT. */
    @Test
    void expiredPremiumUserList_hidesPremiumPrompt() throws Exception {
        mockMvc.perform(get("/api/v1/speaking-prompts").param("size", "50")
                        .with(authAs("zzexpired", user(true, LocalDate.now().minusDays(1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", not(hasItem(PREMIUM_TITLE))));
    }

    @Test
    void activePremiumUserList_showsPremiumPrompt() throws Exception {
        mockMvc.perform(get("/api/v1/speaking-prompts").param("size", "50")
                        .with(authAs("zzpremium", user(true, LocalDate.now().plusDays(30)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(PREMIUM_TITLE)));
    }

    @Test
    void adminList_stillSeesPremiumPrompt() throws Exception {
        mockMvc.perform(get("/api/v1/speaking-prompts").param("size", "50")
                        .with(authAs("zzadmin", adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(PREMIUM_TITLE)));
    }

    @Test
    void anonymousSearch_hidesPremiumPrompt() throws Exception {
        mockMvc.perform(get("/api/v1/speaking-prompts")
                        .param("q", "speaking").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", not(hasItem(PREMIUM_TITLE))))
                .andExpect(jsonPath("$.content[*].title", hasItem(FREE_TITLE)));
    }

    @Test
    void anonymousDetailOfPremiumPrompt_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/speaking-prompts/{id}", premiumId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void adminDetailOfPremiumPrompt_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/speaking-prompts/{id}", premiumId)
                        .with(authAs("zzadmin2", adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(PREMIUM_TITLE));
    }
}
