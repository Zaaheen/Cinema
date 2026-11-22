package com.cinema.dto.admin.movie;

import com.cinema.entity.Genre;

public record AdminGenreResponse(Long id, String name, boolean selectable) {
    public static AdminGenreResponse from(Genre genre) {
        return new AdminGenreResponse(genre.getId(), genre.getName(), genre.isSelectable());
    }
}
