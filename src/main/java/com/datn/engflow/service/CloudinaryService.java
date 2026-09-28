package com.datn.engflow.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
/**
 * Uploads user images and AI-generated audio to Cloudinary and returns the
 * resulting HTTPS URL. Avatars are cropped to a 200x200 face-focused square so
 * the client never has to crop; audio is stored as a Cloudinary video
 * resource. Consumed by the auth/profile controllers and by
 * {@link AiExerciseService} to publish the audio of a generated LISTENING
 * exercise.
 */
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * @param cloudName Cloudinary cloud name from {@code cloudinary.cloud-name}
     * @param apiKey    Cloudinary API key
     * @param apiSecret Cloudinary API secret
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
     * Uploads a profile picture, cropped by Cloudinary to a 200x200
     * face-detected square.
     *
     * @param file the uploaded image
     * @return the HTTPS URL of the stored avatar
     * @throws IllegalArgumentException when the file is empty or not an image
     * @throws IOException              when the upload or the byte read fails
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
     * Uploads an uploaded audio file. Cloudinary stores audio under its video
     * resource type.
     *
     * @param file the uploaded audio part
     * @return the HTTPS URL of the stored audio
     * @throws IllegalArgumentException when the file is empty
     * @throws IOException              when the upload or the byte read fails
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
     * Publishes audio synthesised in-process (TTS output) to Cloudinary.
     *
     * @param audioBytes the raw audio content
     * @param filename   the public id to store under, or null to let
     *                   Cloudinary assign a timestamp-based one
     * @return the HTTPS URL of the stored audio
     * @throws IllegalArgumentException when {@code audioBytes} is null or empty
     * @throws IOException              when the upload fails
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