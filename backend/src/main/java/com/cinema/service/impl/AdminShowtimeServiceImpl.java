package com.cinema.service.impl;

import com.cinema.dto.admin.showtime.AdminShowtimeCreateRequest;
import com.cinema.dto.admin.showtime.AdminShowtimePatchRequest;
import com.cinema.dto.admin.showtime.AdminShowtimeResponse;
import com.cinema.dto.admin.showtime.AdminRoomResponse;
import com.cinema.entity.Movie;
import com.cinema.entity.Room;
import com.cinema.entity.Showtime;
import com.cinema.exception.BusinessException;
import com.cinema.repository.BookingRepository;
import com.cinema.repository.BookingSeatRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoomRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.service.AdminShowtimeService;
import java.sql.SQLException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminShowtimeServiceImpl implements AdminShowtimeService {
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String SCHEDULED = "SCHEDULED";
    private static final String CANCELLED = "CANCELLED";
    private static final String OVERLAP_CONSTRAINT = "ex_showtimes_active_room_occupied";
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final TransactionTemplate transactionTemplate;

    public AdminShowtimeServiceImpl(ShowtimeRepository showtimeRepository, MovieRepository movieRepository,
            RoomRepository roomRepository, BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
            TransactionTemplate transactionTemplate) {
        this.showtimeRepository = showtimeRepository;
        this.movieRepository = movieRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Transactional
    public AdminShowtimeResponse create(AdminShowtimeCreateRequest request) {
        Movie movie = movieRepository.findByIdForUpdate(request.movieId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phim"));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng chiếu"));
        validateMovie(movie);
        validateRoom(room);
        Instant startsAt = request.startsAt().toInstant();
        validateSchedule(startsAt, movie.getDurationMinutes());
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartsAt(startsAt);
        showtime.setEndsAt(startsAt.plusSeconds(movie.getDurationMinutes() * 60L));
        showtime.setStatus(SCHEDULED);
        return saveAndMapConflict(showtime, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminShowtimeResponse> list(LocalDate date) {
        if (date == null) throw BusinessException.badRequest("date là bắt buộc");
        Instant start = date.atStartOfDay(CINEMA_ZONE).toInstant();
        Instant next = date.plusDays(1).atStartOfDay(CINEMA_ZONE).toInstant();
        return showtimeRepository.findAdminByStartsAtBetween(start, next).stream()
                .map(showtime -> AdminShowtimeResponse.from(showtime, hasBookingHistory(showtime.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminRoomResponse> listRooms() {
        return roomRepository.findAll(Sort.by("name").ascending().and(Sort.by("id").ascending()))
                .stream().map(AdminRoomResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminShowtimeResponse get(Long showtimeId) {
        Showtime showtime = showtimeRepository.findAdminById(showtimeId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy suất chiếu"));
        return AdminShowtimeResponse.from(showtime, hasBookingHistory(showtime.getId()));
    }

    @Override
    public AdminShowtimeResponse update(Long showtimeId, AdminShowtimePatchRequest request) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                AdminShowtimeResponse result = transactionTemplate.execute(status -> updateOnce(showtimeId, request));
                if (result != null) return result;
            } catch (RuntimeException exception) {
                if (!isDeadlock(exception) || attempt == 2) throw exception;
            }
        }
        throw new IllegalStateException("Không thể cập nhật suất chiếu");
    }

    private AdminShowtimeResponse updateOnce(Long showtimeId, AdminShowtimePatchRequest request) {
        Showtime showtime = lockedEditableShowtime(showtimeId);
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng chiếu"));
        validateRoom(room);
        Instant startsAt = request.startsAt().toInstant();
        validateSchedule(startsAt, showtime.getMovie().getDurationMinutes());
        showtime.setRoom(room);
        showtime.setStartsAt(startsAt);
        showtime.setEndsAt(startsAt.plusSeconds(showtime.getMovie().getDurationMinutes() * 60L));
        return saveAndMapConflict(showtime, false);
    }

    @Override
    @Transactional
    public AdminShowtimeResponse cancel(Long showtimeId) {
        Showtime showtime = lockedEditableShowtime(showtimeId);
        showtime.setStatus(CANCELLED);
        return saveAndMapConflict(showtime, false);
    }

    private Showtime lockedEditableShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findByIdForUpdate(showtimeId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy suất chiếu"));
        if (!SCHEDULED.equals(showtime.getStatus()) || !showtime.getStartsAt().isAfter(Instant.now())) {
            throw BusinessException.conflict("SHOWTIME_NOT_EDITABLE");
        }
        if (hasBookingHistory(showtime.getId())) throw BusinessException.conflict("SHOWTIME_HAS_BOOKING_HISTORY");
        return showtime;
    }

    private AdminShowtimeResponse saveAndMapConflict(Showtime showtime, boolean hasBookingHistory) {
        try {
            Showtime saved = showtimeRepository.saveAndFlush(showtime);
            return AdminShowtimeResponse.from(saved, hasBookingHistory);
        } catch (DataIntegrityViolationException exception) {
            if (isRoomOverlap(exception)) throw BusinessException.conflict("SHOWTIME_ROOM_CONFLICT");
            throw exception;
        }
    }

    private boolean hasBookingHistory(Long showtimeId) {
        return bookingRepository.existsByShowtimeId(showtimeId) || bookingSeatRepository.existsByShowtimeId(showtimeId);
    }

    private void validateMovie(Movie movie) {
        if (!"COMING_SOON".equals(movie.getStatus()) && !"NOW_SHOWING".equals(movie.getStatus())) {
            throw BusinessException.badRequest("Phim chưa ở trạng thái có thể lên lịch");
        }
        if (movie.getDurationMinutes() <= 0) throw BusinessException.badRequest("Thời lượng phim không hợp lệ");
    }

    private void validateRoom(Room room) {
        if (!"ACTIVE".equals(room.getStatus())) throw BusinessException.badRequest("Phòng chiếu không hoạt động");
    }

    private void validateSchedule(Instant startsAt, int durationMinutes) {
        if (!startsAt.isAfter(Instant.now())) throw BusinessException.badRequest("Suất chiếu phải ở tương lai");
        ZonedDateTime start = startsAt.atZone(CINEMA_ZONE);
        if (start.getSecond() != 0 || start.getNano() != 0 || start.getMinute() % 15 != 0) {
            throw BusinessException.badRequest("Giờ bắt đầu phải theo mốc 15 phút");
        }
        LocalTime startTime = start.toLocalTime();
        if (startTime.isBefore(LocalTime.of(8, 0)) || !startTime.isBefore(LocalTime.of(23, 0))) {
            throw BusinessException.badRequest("Giờ bắt đầu phải trong khoảng 08:00 đến trước 23:00");
        }
        ZonedDateTime end = startsAt.plusSeconds(durationMinutes * 60L).atZone(CINEMA_ZONE);
        if (!end.toLocalDate().equals(start.toLocalDate()) || end.toLocalTime().isAfter(LocalTime.of(23, 0))) {
            throw BusinessException.badRequest("Suất chiếu phải kết thúc không muộn hơn 23:00 cùng ngày");
        }
    }

    private boolean isRoomOverlap(Throwable exception) {
        boolean sqlStateMatches = false;
        boolean constraintMatches = false;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && "23P01".equals(sqlException.getSQLState())) sqlStateMatches = true;
            if (cause instanceof ConstraintViolationException violation
                    && OVERLAP_CONSTRAINT.equals(violation.getConstraintName())) constraintMatches = true;
            if (cause instanceof SQLException && OVERLAP_CONSTRAINT.equals(postgresConstraintName(cause))) {
                constraintMatches = true;
            }
        }
        return sqlStateMatches && constraintMatches;
    }

    private boolean isDeadlock(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && "40P01".equals(sqlException.getSQLState())) return true;
        }
        return false;
    }

    private String postgresConstraintName(Throwable cause) {
        try {
            Method serverError = cause.getClass().getMethod("getServerErrorMessage");
            Object message = serverError.invoke(cause);
            if (message == null) return null;
            Object constraint = message.getClass().getMethod("getConstraint").invoke(message);
            return constraint instanceof String value ? value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
