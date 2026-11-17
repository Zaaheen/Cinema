package com.cinema.service;

import com.cinema.config.CloudinaryProperties;
import com.cinema.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;

@Service
public class CloudinaryPosterService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final CloudinaryProperties properties;
    private final RestClient restClient;

    @Autowired
    public CloudinaryPosterService(CloudinaryProperties properties) {
        this(properties, RestClient.builder());
    }

    CloudinaryPosterService(CloudinaryProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public String uploadPng(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Vui lòng chọn ảnh poster PNG");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw BusinessException.badRequest("Ảnh poster không được vượt quá 5 MB");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw BusinessException.badRequest("Không thể đọc tệp ảnh đã chọn");
        }
        if (!isPng(content)) {
            throw BusinessException.badRequest("Tệp đã chọn không phải ảnh PNG hợp lệ");
        }
        if (!properties.isConfigured()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cloudinary chưa được cấu hình. Hãy kiểm tra CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY và CLOUDINARY_API_SECRET trong backend/.env");
        }

        long timestamp = Instant.now().getEpochSecond();
        String folder = "cinema/posters";
        String signature = sign("folder=" + folder + "&timestamp=" + timestamp, properties.apiSecret());

        MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
        request.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "poster.png";
            }
        });
        request.add("api_key", properties.apiKey());
        request.add("timestamp", Long.toString(timestamp));
        request.add("folder", folder);
        request.add("signature", signature);

        try {
            JsonNode response = restClient.post()
                    .uri("https://api.cloudinary.com/v1_1/{cloudName}/image/upload", properties.cloudName())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            String secureUrl = response == null ? null : response.path("secure_url").asText(null);
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY,
                        "Cloudinary không trả về URL của poster");
            }
            return secureUrl;
        } catch (RestClientException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Không thể tải poster lên Cloudinary. Hãy kiểm tra cấu hình Cloudinary và thử lại");
        }
    }

    private static boolean isPng(byte[] content) {
        if (content.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (content[index] != PNG_SIGNATURE[index]) {
                return false;
            }
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(content)) != null;
        } catch (IOException exception) {
            return false;
        }
    }

    private static String sign(String parameters, String apiSecret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest((parameters + apiSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 không khả dụng", exception);
        }
    }
}
