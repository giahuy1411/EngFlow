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
              {"start":0.0,"end":6.0,"textEn":"Hi, welcome to the café. What can I get for you today?","textVi":"Chào bạn, chào mừng đến quán cà phê. Hôm nay bạn gọi gì?"},
              {"start":6.0,"end":12.0,"textEn":"Hi, can I have a medium latte, please?","textVi":"Chào bạn, cho mình một ly latte cỡ vừa nhé."},
              {"start":12.0,"end":18.0,"textEn":"Sure. Would you like that hot or iced?","textVi":"Được ạ. Bạn muốn uống nóng hay đá?"},
              {"start":18.0,"end":24.0,"textEn":"Iced, please. And could I get oat milk instead of regular milk?","textVi":"Cho mình đá nhé. Và mình có thể đổi sang sữa yến mạch được không?"},
              {"start":24.0,"end":30.0,"textEn":"No problem. Anything else for you today?","textVi":"Không vấn đề gì. Bạn cần thêm gì nữa không?"},
              {"start":30.0,"end":36.0,"textEn":"Yes, one blueberry muffin, please.","textVi":"Vâng, cho mình thêm một cái bánh nho xanh."},
              {"start":36.0,"end":42.0,"textEn":"Great choice. That will be six fifty.","textVi":"Lựa chọn tuyệt vời. Tổng cộng là sáu đô năm mươi."},
              {"start":42.0,"end":48.0,"textEn":"Here you go. Can I pay by card?","textVi":"Của mình đây. Mình trả bằng thẻ được không?"},
              {"start":48.0,"end":54.0,"textEn":"Of course. Just tap here whenever you are ready.","textVi":"Được chứ. Bạn chỉ cần quẹt thẻ ở đây khi sẵn sàng."},
              {"start":54.0,"end":60.0,"textEn":"Done. Thank you so much!","textVi":"Xong rồi. Cảm ơn bạn nhiều lắm!"},
              {"start":60.0,"end":66.0,"textEn":"You are welcome. Your order will be ready in just a minute.","textVi":"Không có gì. Đơn của bạn sẽ có trong ít phút nữa."},
              {"start":66.0,"end":72.0,"textEn":"Perfect, thanks. Have a great day!","textVi":"Tuyệt vời, cảm ơn bạn. Chúc bạn một ngày tốt lành!"}
            ]
            """;

    private final VideoLessonRepository videoLessonRepository;

    @Override
    public void run(String... args) {
        if (videoLessonRepository.count() == 0) {
            videoLessonRepository.save(VideoLesson.builder()
                    .title("Gọi cà phê bằng tiếng Anh")
                    .description("Tình huống thực tế: gọi món ở quán cà phê — luyện shadowing từng câu với phụ đề song ngữ.")
                    .youtubeVideoId("2VeQTuSSiI0")
                    .level(LessonLevel.ELEMENTARY)
                    .category("Giao tiếp")
                    .durationSeconds(247)
                    .transcriptJson(DEMO_TRANSCRIPT)
                    .isPublished(true)
                    .build());
            log.info("Seeded demo video lesson 'Gọi cà phê bằng tiếng Anh'.");
        }
    }
}
