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
 * class CloudinaryService.
 */
public class CloudinaryService {

    private final Cloudinary cloudinary;

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