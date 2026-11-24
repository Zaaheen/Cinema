package com.cinema.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.entity.Movie;
import com.cinema.repository.GenreRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.service.CloudinaryPosterService;
import com.cinema.service.YoutubeTrailerUrlValidator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class AdminMoviePosterPersistenceTests {
    @Test
    void databaseFailureAfterProviderUploadDoesNotReturnASuccessResponse() {
        MovieRepository movies = mock(MovieRepository.class);
        CloudinaryPosterService cloudinary = mock(CloudinaryPosterService.class);
        Movie movie = new Movie(); movie.setId(11L); movie.setTitle("Movie"); movie.setDurationMinutes(100);
        movie.setVersion("2D"); movie.setStatus("COMING_SOON");
        when(movies.existsById(11L)).thenReturn(true);
        when(cloudinary.uploadPosterImage(any())).thenReturn("https://res.cloudinary.com/demo/image/upload/poster.png");
        when(movies.findByIdForUpdate(11L)).thenReturn(Optional.of(movie));
        when(movies.saveAndFlush(any(Movie.class))).thenThrow(new DataIntegrityViolationException("test persistence failure"));
        AdminMovieServiceImpl service = new AdminMovieServiceImpl(movies, mock(GenreRepository.class),
                mock(ShowtimeRepository.class), cloudinary, new YoutubeTrailerUrlValidator(),
                new TransactionTemplate(new NoopTransactionManager()));

        assertThrows(DataIntegrityViolationException.class, () -> service.uploadPoster(11L,
                new MockMultipartFile("file", "poster.png", "image/png", new byte[] {1})));
        verify(cloudinary).uploadPosterImage(any());
    }

    private static class NoopTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
