package com.cinema.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateMovieRequest(
        @NotBlank(message = "Tên phim không được để trống")
        @Size(max = 255, message = "Tên phim tối đa 255 ký tự")
        String title,

        String genre,

        LocalDate releaseDate,

        @Positive(message = "Thời lượng phim phải lớn hơn 0")
        int durationMinutes,

        @NotBlank(message = "Phiên bản phim không được để trống")
        @Size(max = 50, message = "Phiên bản phim tối đa 50 ký tự")
        String version,

        String castText,

        @Size(max = 255, message = "Tên đạo diễn tối đa 255 ký tự")
        String director,

        @Size(max = 255, message = "Tên hãng phim tối đa 255 ký tự")
        String studio,

        String synopsis,

        @Size(max = 500, message = "Trailer URL tối đa 500 ký tự")
        String trailerUrl,

        @Size(max = 500, message = "Poster URL tối đa 500 ký tự")
        @Pattern(
                regexp = "^$|^https?://.+\\.png([?#].*)?$",
                message = "Poster phải là URL PNG kết thúc bằng .png")
        String posterUrl
) {}
