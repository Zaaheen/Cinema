package com.cinema.dto.catalog;

import com.cinema.entity.Genre;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public record GenreResponse(Long id, String name, boolean selectable) {
    public static GenreResponse from(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getName(), genre.isSelectable());
    }

    public static List<GenreResponse> fromAll(Iterable<Genre> genres) {
        java.util.ArrayList<GenreResponse> values = new java.util.ArrayList<>();
        genres.forEach(genre -> values.add(from(genre)));
        values.sort(Comparator.comparing(GenreResponse::name));
        return List.copyOf(values);
    }

    public static String displayNames(List<GenreResponse> genres) {
        return genres.stream().map(GenreResponse::name).collect(Collectors.joining(", "));
    }
}
