package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
/**
 * class MinioService.
 */
public class MinioService {

    private final MinioClient writeClient;
    private final MinioClient readClient;

    @Value("${minio.bucket:speaking-uploads}")
    private String bucket;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.public-url:${minio.url}}")
    private String publicUrl;

    public MinioService(
            @Qualifier("minioWriteClient") MinioClient writeClient,
            @Qualifier("minioReadClient") MinioClient readClient) {
        this.writeClient = writeClient;
        this.readClient = readClient;
    }

    private static final long MAX_SPEAKING_MEDIA_BYTES = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
            "audio/webm", "audio/ogg", "audio/wav", "audio/mpeg", "audio/mp4",
            "video/webm", "video/mp4"
    );

    public String uploadVideo(MultipartFile file) throws Exception {
        validateSpeakingMedia(file);
        String originalName = file.getOriginalFilename() == null ? "recording" : file.getOriginalFilename();
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectName = "speaking/" + UUID.randomUUID() + "_" + safeName;

        boolean found = writeClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            writeClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        writeClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        log.info("Speaking media uploaded to MinIO: {}", objectName);
        return objectName;
    }

    public String createReadUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            String url = readClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
            if (url != null) {
                url = url.replace("minio:9000", "localhost:9000");
            }
            return url;
        } catch (Exception exception) {
            log.error("Could not create media read URL for object {}", objectKey, exception);
            return null;
        }
    }

    public String uploadMedia(String objectKey, MultipartFile file) {
        try {
            boolean found = writeClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                writeClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            writeClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Media uploaded to MinIO: {}", objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Không thể tải tệp lên MinIO", e);
        }
        return objectKey;
    }

    private void validateSpeakingMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tệp ghi âm không được để trống");
        }
        if (file.getSize() > MAX_SPEAKING_MEDIA_BYTES) {
            throw new BadRequestException("Tệp ghi âm vượt quá giới hạn 50 MB");
        }
        if (file.getContentType() == null || ALLOWED_MEDIA_TYPES.stream().noneMatch(t -> file.getContentType().startsWith(t))) {
            throw new BadRequestException("Định dạng media không được hỗ trợ");
        }
    }

    public InputStreamResource getObject(String objectKey) {
        try {
            var response = readClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return new InputStreamResource(response);
        } catch (Exception e) {
            log.error("Could not get object from MinIO: {}", objectKey, e);
            return null;
        }
    }
}
