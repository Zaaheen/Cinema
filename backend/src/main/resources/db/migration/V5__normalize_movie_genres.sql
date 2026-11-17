CREATE TABLE genres (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,
    is_selectable BOOLEAN NOT NULL DEFAULT TRUE
);

-- Preserve any genre values already stored on movies before replacing the
-- single-value column with a relational mapping.
INSERT INTO genres (name, is_selectable)
SELECT DISTINCT BTRIM(genre), FALSE
FROM movies
WHERE genre IS NOT NULL AND BTRIM(genre) <> ''
ON CONFLICT (name) DO NOTHING;

INSERT INTO genres (name, is_selectable) VALUES
    ('Hành động', TRUE),
    ('Hài kịch', TRUE),
    ('Kinh dị', TRUE),
    ('Khoa học viễn tưởng', TRUE),
    ('Tình cảm', TRUE),
    ('Phiêu lưu', TRUE)
ON CONFLICT (name) DO UPDATE SET is_selectable = TRUE;

CREATE TABLE movie_genres (
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres (id),
    PRIMARY KEY (movie_id, genre_id)
);

CREATE INDEX ix_movie_genres_genre ON movie_genres (genre_id);

INSERT INTO movie_genres (movie_id, genre_id)
SELECT movie.id, genre.id
FROM movies movie
JOIN genres genre ON genre.name = BTRIM(movie.genre)
WHERE movie.genre IS NOT NULL AND BTRIM(movie.genre) <> ''
ON CONFLICT DO NOTHING;

ALTER TABLE movies DROP COLUMN genre;
