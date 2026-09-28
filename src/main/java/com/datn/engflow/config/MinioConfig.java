package com.datn.engflow.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Dựng hai bean {@link MinioClient} dùng để chạm tới object storage.
 *
 * <p>Việc tách endpoint ghi và đọc là CỐ Ý: upload đi qua URL nội bộ cụm
 * ({@code minio.url}), còn đọc đi qua {@code minio.public-url} để link tải phân
 * giải được về hostname mà browser cũng truy cập được — nếu dùng chung URL nội bộ
 * thì URL pre-signed trả cho client sẽ không mở nổi. Hai client dùng chung
 * credentials. {@link com.datn.engflow.service.MinioService} inject chúng bằng
 * {@code @Qualifier} — client ghi thêm {@code @Primary} — và dùng client đọc cho
 * mọi lời gọi {@code getObject} phục vụ học viên.</p>
 */
@Configuration
public class MinioConfig {

    /**
     * Tạo client dùng cho upload (bản ghi audio/video, avatar).
     *
     * @param minioUrl  endpoint nội bộ lấy từ {@code minio.url}
     * @param accessKey access key của object storage
     * @param secretKey secret key của object storage
     * @return client gắn với endpoint nội bộ
     */
    @Bean("minioWriteClient")
    @Primary
    public MinioClient minioWriteClient(
            @Value("${minio.url}") String minioUrl,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return createClient(minioUrl, accessKey, secretKey);
    }

    /**
     * Tạo client dùng cho download và URL đọc pre-signed.
     *
     * @param publicUrl endpoint truy cập được từ ngoài, mặc định lấy {@code minio.url}
     * @param accessKey access key của object storage
     * @param secretKey secret key của object storage
     * @return client gắn với endpoint public
     */
    @Bean("minioReadClient")
    public MinioClient minioReadClient(
            @Value("${minio.public-url:${minio.url}}") String publicUrl,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return createClient(publicUrl, accessKey, secretKey);
    }

    /** Builder dùng chung để hai bean chỉ khác nhau đúng ở endpoint. */
    private MinioClient createClient(String endpoint, String accessKey, String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
