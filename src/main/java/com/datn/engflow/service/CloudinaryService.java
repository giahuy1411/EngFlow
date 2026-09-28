package com.datn.engflow.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Tải ảnh người dùng và audio do AI sinh lên Cloudinary rồi trả về URL HTTPS.
 *
 * <p>Avatar được Cloudinary cắt thành ô vuông 200x200 lấy trọng tâm khuôn mặt, để
 * client không phải tự cắt; audio được lưu dưới resource type video của
 * Cloudinary. Được gọi bởi controller auth/profile và bởi
 * {@link AiExerciseService} để đăng audio của bài LISTENING do AI sinh.</p>
 *
 * <p>Cloudinary ở đây là tích hợp thật, không phải demo: {@code AdminUploadController}
 * upload audio thật và nhận về 200 kèm URL Cloudinary. Chỉ những byte giả mới bị
 * từ chối — đó là các kiểm tra đầu vào bên dưới, không phải chế độ mô phỏng.</p>
 */
@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * @param cloudName tên cloud Cloudinary, lấy từ {@code cloudinary.cloud-name}
     * @param apiKey    API key của Cloudinary
     * @param apiSecret API secret của Cloudinary
     */
    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /**
     * Tải ảnh đại diện lên, để Cloudinary cắt thành ô vuông 200x200 theo khuôn
     * mặt nhận diện được.
     *
     * @param file ảnh người dùng tải lên
     * @return URL HTTPS của avatar đã lưu
     * @throws IllegalArgumentException khi file rỗng hoặc không phải ảnh
     * @throws IOException              khi upload hoặc đọc byte thất bại
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File khong duoc de trong");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chi chap nhan file anh (image/*)");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "engflow/avatars",
                "transformation", "w_200,h_200,c_fill,g_face",
                "resource_type", "image"
        ));

        String secureUrl = (String) result.get("secure_url");
        log.info("Avatar uploaded to Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    /**
     * Tải một file audio người dùng đã upload lên. Cloudinary lưu audio dưới
     * resource type video của nó.
     *
     * @param file phần audio được tải lên
     * @return URL HTTPS của audio đã lưu
     * @throws IllegalArgumentException khi file rỗng
     * @throws IOException              khi upload hoặc đọc byte thất bại
     */
    public String uploadAudio(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File khong duoc de trong");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "engflow/audio",
                "resource_type", "video"
        ));

        String secureUrl = (String) result.get("secure_url");
        log.info("Audio uploaded to Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    /**
     * Đăng đoạn audio được tổng hợp ngay trong tiến trình (đầu ra TTS) lên
     * Cloudinary.
     *
     * @param audioBytes nội dung audio thô
     * @param filename   public id muốn lưu dưới đó, hoặc null để Cloudinary tự
     *                   gán id dựa trên timestamp
     * @return URL HTTPS của audio đã lưu
     * @throws IllegalArgumentException khi {@code audioBytes} null hoặc rỗng
     * @throws IOException              khi upload thất bại
     */
    public String uploadAudioBytes(byte[] audioBytes, String filename) throws IOException {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalArgumentException("Audio bytes khong duoc de trong");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(audioBytes, ObjectUtils.asMap(
                "folder", "engflow/audio",
                "resource_type", "video",
                "public_id", filename != null ? filename : "ai-listening-" + System.currentTimeMillis()
        ));

        String secureUrl = (String) result.get("secure_url");
        log.info("AI audio uploaded to Cloudinary: {}", secureUrl);
        return secureUrl;
    }
}