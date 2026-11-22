-- Keep movies.genre as the legacy public-catalog source while introducing a
-- normalized relation for future ADMIN movie management.  This migration is
-- deliberately additive: it does not rewrite, truncate, or remove legacy data.

CREATE TABLE genres (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) COLLATE "C" NOT NULL,
    is_selectable BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ux_genres_name UNIQUE (name)
);

CREATE TABLE movie_genres (
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres (id) ON DELETE RESTRICT,
    CONSTRAINT pk_movie_genres PRIMARY KEY (movie_id, genre_id)
);

CREATE INDEX ix_movie_genres_genre ON movie_genres (genre_id);

-- The only selectable genres approved for ADMIN movie-management UI.
INSERT INTO genres (name, is_selectable) VALUES
    ('Hành động', TRUE),
    ('Hài kịch', TRUE),
    ('Kinh dị', TRUE),
    ('Khoa học viễn tưởng', TRUE),
    ('Tình cảm', TRUE),
    ('Phiêu lưu', TRUE)
ON CONFLICT (name) DO UPDATE SET is_selectable = TRUE;

-- Legacy values are atomic strings.  Trim only for the normalized mapping;
-- delimiters, aliases, case, and accents are intentionally not transformed.
INSERT INTO genres (name, is_selectable)
SELECT DISTINCT BTRIM(m.genre), FALSE
FROM movies m
WHERE m.genre IS NOT NULL
  AND BTRIM(m.genre) <> ''
ON CONFLICT (name) DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
JOIN genres g ON g.name = (BTRIM(m.genre) COLLATE "C")
WHERE m.genre IS NOT NULL
  AND BTRIM(m.genre) <> ''
ON CONFLICT (movie_id, genre_id) DO NOTHING;
