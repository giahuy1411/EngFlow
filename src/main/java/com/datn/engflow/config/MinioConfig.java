package com.datn.engflow.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
/**
 * class MinioConfig.
 */
public class MinioConfig {

    @Bean("minioWriteClient")
    @Primary
    public MinioClient minioWriteClient(
            @Value("${minio.url}") String minioUrl,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return createClient(minioUrl, accessKey, secretKey);
    }

    @Bean("minioReadClient")
    public MinioClient minioReadClient(
            @Value("${minio.public-url:${minio.url}}") String publicUrl,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return createClient(publicUrl, accessKey, secretKey);
    }

    private MinioClient createClient(String endpoint, String accessKey, String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
