package com.cinema.dto.admin.movie;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record AdminMovieRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive Long primaryGenreId,
        @NotEmpty @Size(max = 6) List<@NotNull @Positive Long> genreIds,
        LocalDate releaseDate,
        @NotNull @Positive Integer durationMinutes,
        @NotBlank @Size(max = 50) String version,
        String castText,
        @Size(max = 255) String director,
        @Size(max = 255) String studio,
        String synopsis,
        @Size(max = 500) String posterUrl,
        @Size(max = 500) String backdropUrl,
        @Size(max = 10) String ageRating) {
}
