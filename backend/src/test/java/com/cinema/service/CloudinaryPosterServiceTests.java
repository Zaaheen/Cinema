package com.cinema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.cinema.config.CloudinaryProperties;
import com.cinema.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CloudinaryPosterServiceTests {
    @Test
    void uploadsValidatedPngAndOnlyReturnsSecureUrl() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudinaryPosterService service = new CloudinaryPosterService(configuredProperties(), builder.build());
        server.expect(requestTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("{\"secure_url\":\"https://res.cloudinary.com/demo-cloud/image/upload/v1/poster.png\",\"public_id\":\"secret-id\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals("https://res.cloudinary.com/demo-cloud/image/upload/v1/poster.png",
                service.uploadPng(png("poster.PNG", "image/png", 1, 1)));
        server.verify();
    }

    @Test
    void rejectsBadFileMetadataAndContentBeforeProviderCall() throws Exception {
        CloudinaryPosterService service = new CloudinaryPosterService(configuredProperties(), RestClient.create());
        assertBadRequest(() -> service.uploadPng(new MockMultipartFile("file", "poster.jpg", "image/png", pngBytes(1, 1))));
        assertBadRequest(() -> service.uploadPng(new MockMultipartFile("file", "poster.png", "image/jpeg", pngBytes(1, 1))));
        assertBadRequest(() -> service.uploadPng(new MockMultipartFile("file", "poster.png", "image/png", new byte[0])));
        assertBadRequest(() -> service.uploadPng(new MockMultipartFile("file", "poster.png", "image/png", new byte[CloudinaryPosterService.MAX_FILE_SIZE_BYTES > Integer.MAX_VALUE ? 0 : (int) CloudinaryPosterService.MAX_FILE_SIZE_BYTES + 1])));
        assertBadRequest(() -> service.uploadPng(new MockMultipartFile("file", "poster.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00})));
    }

    @Test
    void rejectsImagesOutsideDimensionLimitsBeforeProviderCall() throws Exception {
        CloudinaryPosterService service = new CloudinaryPosterService(configuredProperties(), RestClient.create());
        assertBadRequest(() -> service.uploadPng(png("wide.png", "image/png", 4097, 1)));
        assertBadRequest(() -> service.uploadPng(png("pixel-limit.png", "image/png", 4000, 3001)));
    }

    @Test
    void reportsUnavailableConfigurationAndProviderFailuresWithoutLeakingDetails() throws Exception {
        CloudinaryPosterService unavailable = new CloudinaryPosterService(
                new CloudinaryProperties("", "", "", Duration.ofSeconds(1), Duration.ofSeconds(1)), RestClient.create());
        assertStatus(503, () -> unavailable.uploadPng(png("poster.png", "image/png", 1, 1)));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudinaryPosterService failing = new CloudinaryPosterService(configuredProperties(), builder.build());
        server.expect(requestTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload")).andRespond(withServerError());
        assertStatus(502, () -> failing.uploadPng(png("poster.png", "image/png", 1, 1)));
        server.verify();
    }

    @Test
    void rejectsMalformedProviderResponsesAndTimeouts() throws Exception {
        RestClient.Builder malformedBuilder = RestClient.builder();
        MockRestServiceServer malformedServer = MockRestServiceServer.bindTo(malformedBuilder).build();
        CloudinaryPosterService malformed = new CloudinaryPosterService(configuredProperties(), malformedBuilder.build());
        malformedServer.expect(requestTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload"))
                .andRespond(withSuccess("{\"secure_url\":\"http://unexpected.test/poster.png\"}", MediaType.APPLICATION_JSON));
        assertStatus(502, () -> malformed.uploadPng(png("poster.png", "image/png", 1, 1)));
        malformedServer.verify();

        RestClient.Builder timeoutBuilder = RestClient.builder();
        MockRestServiceServer timeoutServer = MockRestServiceServer.bindTo(timeoutBuilder).build();
        CloudinaryPosterService timeout = new CloudinaryPosterService(configuredProperties(), timeoutBuilder.build());
        timeoutServer.expect(requestTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload"))
                .andRespond(request -> { throw new SocketTimeoutException("test timeout"); });
        assertStatus(502, () -> timeout.uploadPng(png("poster.png", "image/png", 1, 1)));
        timeoutServer.verify();
    }

    private static CloudinaryProperties configuredProperties() {
        return new CloudinaryProperties("demo-cloud", "api-key", "api-secret", Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    private static MockMultipartFile png(String name, String type, int width, int height) throws Exception {
        return new MockMultipartFile("file", name, type, pngBytes(width, height));
    }

    private static byte[] pngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static void assertBadRequest(ThrowingAction action) {
        assertStatus(400, action);
    }

    private static void assertStatus(int status, ThrowingAction action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertEquals(status, exception.getStatus().value());
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
