package com.cinema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.cinema.config.CloudinaryProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CloudinaryPosterServiceTests {

    @Test
    void uploadsPngAndReadsSecureUrlFromJackson3Response() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudinaryPosterService service = new CloudinaryPosterService(
                new CloudinaryProperties("demo-cloud", "api-key", "api-secret"), builder);

        server.expect(requestTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess(
                        "{\"secure_url\":\"https://res.cloudinary.com/demo-cloud/image/upload/poster.png\"}",
                        MediaType.APPLICATION_JSON));

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", png);
        MockMultipartFile file = new MockMultipartFile("file", "poster.png", "image/png", png.toByteArray());

        assertEquals("https://res.cloudinary.com/demo-cloud/image/upload/poster.png", service.uploadPng(file));
        server.verify();
    }
}
