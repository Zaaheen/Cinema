package com.cinema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class YoutubeTrailerUrlValidatorTests {
    private final YoutubeTrailerUrlValidator validator = new YoutubeTrailerUrlValidator();

    @Test
    void normalizesApprovedYoutubePatterns() {
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk",
                validator.normalize("https://youtube.com/watch?v=abcdefghijk"));
        assertEquals("https://www.youtube.com/watch?v=ABCDEFGHIJK",
                validator.normalize("https://youtu.be/ABCDEFGHIJK"));
    }

    @Test
    void rejectsUnsafeOrUnsupportedTrailerUrls() {
        for (String value : List.of(
                "http://youtube.com/watch?v=abcdefghijk",
                "https://youtube.com.evil.test/watch?v=abcdefghijk",
                "https://user@youtube.com/watch?v=abcdefghijk",
                "https://youtube.com:443/watch?v=abcdefghijk",
                "https://youtube.com/watch?v=abcdefghijk&next=https://evil.test",
                "https://youtube.com/watch?v=abcdefghijk#fragment",
                "https://youtube.com/watch?v=short",
                "<iframe src=\"https://youtube.com/watch?v=abcdefghijk\"></iframe>")) {
            assertThrows(BusinessException.class, () -> validator.normalize(value), value);
        }
    }
}
