package com.datn.engflow.config;

import com.datn.engflow.model.entity.VideoLesson;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.VideoLessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * class VideoLessonSeeder — seeds one demo video lesson (transcript authored
 * by the teacher, YouTube embedded only) so the feature is testable out of the box.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class VideoLessonSeeder implements CommandLineRunner {

    private static final String DEMO_TRANSCRIPT = """
            [
              {"start":0.0,"end":3.6,"textEn":"Hello, and welcome to English daily life!","textVi":"Xin chào và chào mừng bạn đến với tiếng Anh đời sống hằng ngày!"},
              {"start":3.6,"end":7.4,"textEn":"Today we are ordering coffee at a cafe.","textVi":"Hôm nay chúng ta sẽ gọi cà phê ở một quán."},
              {"start":7.4,"end":11.0,"textEn":"Can I have a large latte, please?","textVi":"Cho tôi một ly latte lớn, please?"},
              {"start":11.0,"end":14.2,"textEn":"Sure. Would you like anything to eat?","textVi":"Được ạ. Bạn có muốn thêm đồ ăn không?"},
              {"start":14.2,"end":17.8,"textEn":"Just the coffee, thanks. How much is it?","textVi":"Chỉ cà phê thôi, cảm ơn. Bao nhiêu tiền vậy?"},
              {"start":17.8,"end":21.0,"textEn":"That will be four dollars fifty.","textVi":"Tổng cộng là bốn đô rưỡi."},
              {"start":21.0,"end":24.6,"textEn":"Here you are. Keep the change.","textVi":"Của bạn đây. Cứ thối lại nhé."},
              {"start":24.6,"end":28.0,"textEn":"Thanks a lot. Have a nice day!","textVi":"Cảm ơn nhiều. Chúc một ngày tốt lành!"}
            ]
            """;

    private final VideoLessonRepository videoLessonRepository;

    @Override
    public void run(String... args) {
        if (videoLessonRepository.count() == 0) {
            videoLessonRepository.save(VideoLesson.builder()
                    .title("Gọi cà phê bằng tiếng Anh")
                    .description("Tình huống thực tế: gọi món ở quán cà phê — luyện shadowing từng câu với phụ đề song ngữ.")
                    .youtubeVideoId("aqc-5sE2SNQ")
                    .level(LessonLevel.ELEMENTARY)
                    .category("Giao tiếp")
                    .durationSeconds(28)
                    .transcriptJson(DEMO_TRANSCRIPT)
                    .isPublished(true)
                    .build());
            log.info("Seeded demo video lesson 'Gọi cà phê bằng tiếng Anh'.");
        }
    }
}
