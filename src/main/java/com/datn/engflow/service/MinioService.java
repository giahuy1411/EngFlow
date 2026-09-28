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
 * Stores the media blobs that recordings produce (speaking uploads, video
 * lesson assets) in MinIO and hands back presigned read URLs. The bucket is
 * private; nothing is served without a URL minted here. Talks to two clients —
 * {@code minioWriteClient} for uploads, {@code minioReadClient} for reads — so
 * a deployment can write and read through different endpoints.
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

    /**
     * @param writeClient client used for uploads, injected as
     *                    {@code minioWriteClient}
     * @param readClient  client used for presigned URLs and object reads,
     *                    injected as {@code minioReadClient}
     */
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

    /**
     * Stores a learner recording under a fresh UUID-prefixed key after checking
     * size and MIME type. The original filename is kept only as a sanitised
     * suffix, so a user-supplied name can never steer the object key outside
     * the {@code speaking/} prefix.
     *
     * @param file the uploaded audio or video part
     * @return the object key to persist on the submission row
     * @throws BadRequestException   when the file is empty, over 50 MB, or not
     *                               an allowed media type
     * @throws Exception            when MinIO rejects the upload
     */
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

    /**
     * Mints a one-hour presigned GET URL so a browser can play a stored
     * recording. The generated URL may point at the in-network MinIO host, so
     * it is rewritten to the host the browser can actually reach. Fails soft:
     * callers get null and render the row without playable media.
     *
     * @param objectKey the key previously returned by an upload method
     * @return the presigned URL, or null for a blank key or a MinIO error
     */
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

    /**
     * Stores a file at a caller-chosen key. Unlike {@link #uploadVideo} this
     * path applies no size or MIME check — it is used for curated content
     * (video lesson assets), not for user recordings.
     *
     * @param objectKey the full key to write, including any prefix
     * @param file      the content to store
     * @return {@code objectKey}, echoed back for call-site convenience
     * @throws RuntimeException when MinIO rejects the upload
     */
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

    /**
     * Trust-boundary check for anything a learner uploads. The declared MIME
     * type is client-controlled, so it is a filter, not proof — it is applied
     * anyway to keep obviously wrong content out of the bucket and to reject
     * oversized recordings before the body is buffered.
     *
     * @param file the uploaded part
     * @throws BadRequestException when the file is null, empty, over 50 MB, or
     *                             its content type is outside the allow-list
     */
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

    /**
     * Opens a stored object for streaming back to a client. Used by the media
     * proxy so a private object is never exposed directly.
     *
     * @param objectKey the key to read
     * @return a resource wrapping the object stream, or null when MinIO fails
     */
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
