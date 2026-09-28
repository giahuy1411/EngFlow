package com.datn.engflow.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Builds the two {@link MinioClient} beans used to reach object storage.
 *
 * <p>Writes and reads use different endpoints on purpose: uploads go to the
 * in-cluster URL, while reads go through {@code minio.public-url} so downloads
 * resolve to a hostname the browser can also reach. Both share the same
 * credentials. {@link com.datn.engflow.service.MinioService} injects them by
 * {@code @Qualifier} — the write client is additionally {@code @Primary} — and
 * uses the read client for every {@code getObject} call it serves to
 * learners.</p>
 */
@Configuration
public class MinioConfig {

    /**
     * Creates the client used for uploads (audio/video recordings, avatars).
     *
     * @param minioUrl   internal endpoint from {@code minio.url}
     * @param accessKey  object-storage access key
     * @param secretKey  object-storage secret key
     * @return a client bound to the internal endpoint
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
     * Creates the client used for downloads and pre-signed read URLs.
     *
     * @param publicUrl externally reachable endpoint, defaulting to {@code minio.url}
     * @param accessKey object-storage access key
     * @param secretKey object-storage secret key
     * @return a client bound to the public endpoint
     */
    @Bean("minioReadClient")
    public MinioClient minioReadClient(
            @Value("${minio.public-url:${minio.url}}") String publicUrl,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return createClient(publicUrl, accessKey, secretKey);
    }

    /** Shared builder so the two beans differ only in endpoint. */
    private MinioClient createClient(String endpoint, String accessKey, String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
