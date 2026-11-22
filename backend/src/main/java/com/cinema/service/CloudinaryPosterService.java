package com.cinema.service;

import com.cinema.config.CloudinaryProperties;
import com.cinema.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

@Service
public class CloudinaryPosterService {
    static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_WIDTH = 4096;
    private static final int MAX_HEIGHT = 4096;
    private static final long MAX_PIXELS = 12_000_000L;
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final String FOLDER = "cinema/movie-posters";
    private static final String TAG = "cinema-movie-poster";

    private final CloudinaryProperties properties;
    private final RestClient restClient;

    @Autowired
    public CloudinaryPosterService(CloudinaryProperties properties) {
        this(properties, RestClient.builder().requestFactory(requestFactory(properties)).build());
    }

    CloudinaryPosterService(CloudinaryProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public String uploadPng(MultipartFile file) {
        byte[] content = validateAndRead(file);
        if (!properties.isConfigured()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ tải poster hiện chưa sẵn sàng");
        }

        long timestamp = Instant.now().getEpochSecond();
        String signature = sign("folder=" + FOLDER + "&tags=" + TAG + "&timestamp=" + timestamp,
                properties.apiSecret());
        MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
        request.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "poster.png";
            }
        });
        request.add("api_key", properties.apiKey());
        request.add("timestamp", Long.toString(timestamp));
        request.add("folder", FOLDER);
        request.add("tags", TAG);
        request.add("signature", signature);

        try {
            JsonNode response = restClient.post()
                    .uri("https://api.cloudinary.com/v1_1/{cloudName}/image/upload", properties.cloudName())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            String secureUrl = response == null ? null : response.path("secure_url").asText(null);
            if (!isExpectedCloudinaryUrl(secureUrl)) {
                throw providerFailure();
            }
            return secureUrl;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw providerFailure();
        }
    }

    private byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Vui lòng chọn ảnh poster PNG");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw BusinessException.badRequest("Ảnh poster không được vượt quá 5 MiB");
        }
        if (!hasPngFilename(file.getOriginalFilename()) || !isPngMediaType(file.getContentType())) {
            throw BusinessException.badRequest("Poster phải là tệp PNG hợp lệ");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw BusinessException.badRequest("Không thể đọc tệp poster");
        }
        if (content.length == 0 || content.length > MAX_FILE_SIZE_BYTES || !hasPngSignature(content)) {
            throw BusinessException.badRequest("Poster phải là tệp PNG hợp lệ");
        }
        validatePngDimensionsAndDecode(content);
        return content;
    }

    private void validatePngDimensionsAndDecode(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw BusinessException.badRequest("Poster PNG không thể giải mã");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_WIDTH || height > MAX_HEIGHT
                        || (long) width * height > MAX_PIXELS) {
                    throw BusinessException.badRequest("Kích thước poster vượt quá giới hạn cho phép");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null || decoded.getWidth() != width || decoded.getHeight() != height) {
                    throw BusinessException.badRequest("Poster PNG không thể giải mã");
                }
            } finally {
                reader.dispose();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.badRequest("Poster PNG không thể giải mã");
        }
    }

    private boolean isExpectedCloudinaryUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && "res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    && uri.getRawUserInfo() == null && uri.getPort() == -1
                    && uri.getPath() != null && uri.getPath().startsWith("/" + properties.cloudName() + "/image/upload/");
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static JdkClientHttpRequestFactory requestFactory(CloudinaryProperties properties) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(properties.effectiveConnectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(properties.effectiveReadTimeout());
        return factory;
    }

    private static boolean hasPngFilename(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".png");
    }

    private static boolean isPngMediaType(String contentType) {
        try {
            return contentType != null && MediaType.parseMediaType(contentType).equalsTypeAndSubtype(MediaType.IMAGE_PNG);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean hasPngSignature(byte[] content) {
        if (content.length < PNG_SIGNATURE.length) return false;
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (content[index] != PNG_SIGNATURE[index]) return false;
        }
        return true;
    }

    private static BusinessException providerFailure() {
        return new BusinessException(HttpStatus.BAD_GATEWAY, "Không thể tải poster lên dịch vụ lưu trữ");
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
