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
    private static final String BANNER_FOLDER = "cinema/movie-banners";
    private static final String BANNER_TAG = "cinema-movie-banner";

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

    public String uploadPosterImage(MultipartFile file) {
        return uploadImage(file, FOLDER, TAG, "poster", "Poster", false);
    }

    public String uploadBannerImage(MultipartFile file) {
        return uploadImage(file, BANNER_FOLDER, BANNER_TAG, "banner", "Banner", true);
    }

    private String uploadImage(MultipartFile file, String folder, String tag, String basename,
            String label, boolean landscape) {
        ValidatedImage image = validateAndRead(file, label, landscape);
        if (!properties.isConfigured()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ tải ảnh hiện chưa sẵn sàng");
        }

        long timestamp = Instant.now().getEpochSecond();
        String signature = sign("folder=" + folder + "&tags=" + tag + "&timestamp=" + timestamp,
                properties.apiSecret());
        MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
        request.add("file", new ByteArrayResource(image.content()) {
            @Override
            public String getFilename() {
                return basename + "." + image.format().extension;
            }
        });
        request.add("api_key", properties.apiKey());
        request.add("timestamp", Long.toString(timestamp));
        request.add("folder", folder);
        request.add("tags", tag);
        request.add("signature", signature);

        try {
            JsonNode response = restClient.post()
                    .uri("https://api.cloudinary.com/v1_1/{cloudName}/image/upload", properties.cloudName())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            String secureUrl = response == null ? null : response.path("secure_url").asText(null);
            if (!isExpectedCloudinaryUrl(secureUrl) || secureUrl.length() > 500) {
                throw providerFailure();
            }
            return secureUrl;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw providerFailure();
        }
    }

    private ValidatedImage validateAndRead(MultipartFile file, String label, boolean landscape) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Vui lòng chọn ảnh " + label);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw BusinessException.badRequest("Ảnh " + label + " không được vượt quá 5 MiB");
        }
        ImageFormat format = ImageFormat.from(file.getOriginalFilename(), file.getContentType());
        if (format == null) throw BusinessException.badRequest(label + " phải là ảnh PNG, JPG/JPEG hoặc WebP hợp lệ");
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw BusinessException.badRequest("Không thể đọc tệp " + label);
        }
        if (content.length == 0 || content.length > MAX_FILE_SIZE_BYTES || !format.hasSignature(content)) {
            throw BusinessException.badRequest(label + " phải là ảnh " + format.label + " hợp lệ");
        }
        validateImageDimensionsAndDecode(content, format, label, landscape);
        return new ValidatedImage(content, format);
    }

    private void validateImageDimensionsAndDecode(byte[] content, ImageFormat format, String label, boolean landscape) {
        if (format == ImageFormat.WEBP) {
            int[] dimensions = webpDimensions(content, label);
            validateDimensions(dimensions[0], dimensions[1], label, landscape);
            return;
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw BusinessException.badRequest(label + " không thể giải mã");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height, label, landscape);
                BufferedImage decoded = reader.read(0);
                if (decoded == null || decoded.getWidth() != width || decoded.getHeight() != height) {
                    throw BusinessException.badRequest(label + " không thể giải mã");
                }
            } finally {
                reader.dispose();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.badRequest(label + " không thể giải mã");
        }
    }

    private static void validateDimensions(int width, int height, String label, boolean landscape) {
        if (width <= 0 || height <= 0 || width > MAX_WIDTH || height > MAX_HEIGHT
                || (long) width * height > MAX_PIXELS) {
            throw BusinessException.badRequest("Kích thước " + label + " vượt quá giới hạn cho phép");
        }
        if (landscape && (long) width * 10 < (long) height * 14) {
            throw BusinessException.badRequest("Banner cần là ảnh ngang (tỉ lệ tối thiểu 7:5)");
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

    private static int octet(byte[] bytes, int index) { return bytes[index] & 0xff; }

    private static int uint16(byte[] bytes, int index) {
        return octet(bytes, index) | octet(bytes, index + 1) << 8;
    }

    private static int uint24(byte[] bytes, int index) {
        return octet(bytes, index) | octet(bytes, index + 1) << 8 | octet(bytes, index + 2) << 16;
    }

    private static long uint32(byte[] bytes, int index) {
        return (long) octet(bytes, index) | (long) octet(bytes, index + 1) << 8
                | (long) octet(bytes, index + 2) << 16 | (long) octet(bytes, index + 3) << 24;
    }

    private static boolean ascii(byte[] bytes, int index, String value) {
        if (index < 0 || index + value.length() > bytes.length) return false;
        for (int i = 0; i < value.length(); i++) {
            if (bytes[index + i] != value.charAt(i)) return false;
        }
        return true;
    }

    private static int[] webpDimensions(byte[] bytes, String label) {
        if (bytes.length < 20 || !ascii(bytes, 0, "RIFF") || !ascii(bytes, 8, "WEBP")
                || uint32(bytes, 4) != bytes.length - 8L) {
            throw BusinessException.badRequest(label + " WebP không hợp lệ");
        }
        int offset = 12;
        int[] canvas = null;
        int[] image = null;
        while (offset + 8 <= bytes.length) {
            String chunk = new String(bytes, offset, 4, StandardCharsets.US_ASCII);
            long size = uint32(bytes, offset + 4);
            long end = (long) offset + 8 + size;
            if (end > bytes.length || end + (size & 1) > bytes.length) {
                throw BusinessException.badRequest(label + " WebP không hợp lệ");
            }
            int start = offset + 8;
            if (chunk.equals("VP8X") && size >= 10) {
                canvas = new int[] {uint24(bytes, start + 4) + 1, uint24(bytes, start + 7) + 1};
            } else if (chunk.equals("VP8 ") && size >= 10 && octet(bytes, start + 3) == 0x9d
                    && octet(bytes, start + 4) == 0x01 && octet(bytes, start + 5) == 0x2a) {
                image = new int[] {uint16(bytes, start + 6) & 0x3fff, uint16(bytes, start + 8) & 0x3fff};
            } else if (chunk.equals("VP8L") && size >= 5 && octet(bytes, start) == 0x2f) {
                image = new int[] {1 + octet(bytes, start + 1) + ((octet(bytes, start + 2) & 0x3f) << 8),
                        1 + (octet(bytes, start + 2) >> 6) + (octet(bytes, start + 3) << 2)
                                + ((octet(bytes, start + 4) & 0x0f) << 10)};
            }
            offset = (int) (end + (size & 1));
        }
        if (offset != bytes.length || image == null) {
            throw BusinessException.badRequest(label + " WebP không hợp lệ");
        }
        if (canvas != null && (canvas[0] < image[0] || canvas[1] < image[1])) {
            throw BusinessException.badRequest(label + " WebP không hợp lệ");
        }
        return canvas == null ? image : canvas;
    }

    private record ValidatedImage(byte[] content, ImageFormat format) {}

    private enum ImageFormat {
        PNG("png", "PNG", "image/png"), JPEG("jpg", "JPG/JPEG", "image/jpeg"), WEBP("webp", "WebP", "image/webp");

        private final String extension;
        private final String label;
        private final String mediaType;

        ImageFormat(String extension, String label, String mediaType) {
            this.extension = extension;
            this.label = label;
            this.mediaType = mediaType;
        }

        static ImageFormat from(String filename, String contentType) {
            if (filename == null || contentType == null) return null;
            String lower = filename.toLowerCase(Locale.ROOT);
            try {
                MediaType parsed = MediaType.parseMediaType(contentType);
                for (ImageFormat format : values()) {
                    boolean nameMatches = format == JPEG ? lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                            : lower.endsWith("." + format.extension);
                    if (nameMatches && parsed.equalsTypeAndSubtype(MediaType.parseMediaType(format.mediaType))) return format;
                }
            } catch (IllegalArgumentException exception) {
                return null;
            }
            return null;
        }

        boolean hasSignature(byte[] bytes) {
            if (this == PNG) {
                if (bytes.length < PNG_SIGNATURE.length) return false;
                for (int index = 0; index < PNG_SIGNATURE.length; index++) {
                    if (bytes[index] != PNG_SIGNATURE[index]) return false;
                }
                return true;
            }
            if (this == JPEG) return bytes.length >= 4 && octet(bytes, 0) == 0xff
                    && octet(bytes, 1) == 0xd8 && octet(bytes, 2) == 0xff;
            return bytes.length >= 20 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP");
        }
    }

    private static BusinessException providerFailure() {
        return new BusinessException(HttpStatus.BAD_GATEWAY, "Không thể tải ảnh lên dịch vụ lưu trữ");
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
