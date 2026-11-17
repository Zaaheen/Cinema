package com.cinema.service.impl;

import com.cinema.dto.catalog.AdminShowtimeResponse;
import com.cinema.dto.catalog.RoomResponse;
import com.cinema.dto.catalog.ShowtimeSlotRequest;
import com.cinema.entity.Movie;
import com.cinema.entity.Room;
import com.cinema.entity.Showtime;
import com.cinema.exception.BusinessException;
import com.cinema.repository.BookingRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoomRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.service.AdminShowtimeService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminShowtimeServiceImpl implements AdminShowtimeService {

    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Duration ROOM_BUFFER = Duration.ofMinutes(15);
    private static final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(23, 0);

    private final RoomRepository roomRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;

    public AdminShowtimeServiceImpl(
            RoomRepository roomRepository,
            MovieRepository movieRepository,
            ShowtimeRepository showtimeRepository,
            BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> rooms() {
        return roomRepository.findAllByStatusOrderByNameAsc("ACTIVE")
                .stream().map(RoomResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminShowtimeResponse> showtimes(LocalDate date) {
        Instant startOfDay = date.atStartOfDay(CINEMA_ZONE).toInstant();
        Instant startOfNextDay = date.plusDays(1).atStartOfDay(CINEMA_ZONE).toInstant();
        List<Showtime> showtimes = showtimeRepository.findAllOverlappingDay(startOfDay, startOfNextDay);
        if (showtimes.isEmpty()) return List.of();

        List<Long> ids = showtimes.stream().map(Showtime::getId).toList();
        Set<Long> withBookings = new HashSet<>(bookingRepository.findShowtimeIdsWithBookings(ids));
        return showtimes.stream()
                .map(showtime -> AdminShowtimeResponse.from(showtime, withBookings.contains(showtime.getId())))
                .toList();
    }

    @Override
    public List<AdminShowtimeResponse> createShowtimes(Long movieId, List<ShowtimeSlotRequest> slots) {
        Movie movie = movieRepository.findByIdForUpdate(movieId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phim"));
        List<Showtime> created = createSchedules(movie, slots);
        return created.stream().map(showtime -> AdminShowtimeResponse.from(showtime, false)).toList();
    }

    @Override
    public void createForMovie(Movie movie, List<ShowtimeSlotRequest> slots) {
        createSchedules(movie, slots);
    }

    @Override
    public void updateMovieDuration(Movie movie, int durationMinutes) {
        if (movie.getDurationMinutes() == durationMinutes || movie.getId() == null) return;
        lockRooms(roomRepository.findAll().stream().map(Room::getId).sorted().toList());
        List<Showtime> affected = showtimeRepository.findUpcomingScheduledByMovie(movie.getId(), Instant.now());
        if (affected.isEmpty()) return;

        List<Long> showtimeIds = affected.stream().map(Showtime::getId).toList();
        Set<Long> affectedIds = new HashSet<>(showtimeIds);
        if (!bookingRepository.findShowtimeIdsWithBookings(showtimeIds).isEmpty()) {
            throw BusinessException.conflict("Không thể đổi thời lượng vì đã có suất chiếu với booking hoặc vé");
        }

        List<Slot> recalculated = affected.stream().map(showtime -> {
            Instant endsAt = showtime.getStartsAt().plus(Duration.ofMinutes(durationMinutes));
            ensureEndInsideBusinessHours(showtime.getStartsAt(), endsAt);
            return new Slot(showtime.getRoom(), showtime.getStartsAt(), endsAt);
        }).toList();
        ensureNoBatchConflicts(recalculated);
        for (Slot slot : recalculated) ensureNoDatabaseConflict(slot, affectedIds);

        for (int index = 0; index < affected.size(); index++) {
            affected.get(index).setEndsAt(recalculated.get(index).endsAt());
        }
        try {
            showtimeRepository.saveAllAndFlush(affected);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Thời lượng mới gây trùng lịch phòng chiếu");
        }
    }

    @Override
    public AdminShowtimeResponse updateShowtime(Long showtimeId, ShowtimeSlotRequest request) {
        Showtime observed = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy suất chiếu"));
        lockRooms(List.of(observed.getRoom().getId(), request.roomId()));
        Showtime showtime = showtimeRepository.findByIdForUpdate(showtimeId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy suất chiếu"));
        if (!showtime.getRoom().getId().equals(observed.getRoom().getId())) {
            throw BusinessException.conflict("Suất chiếu vừa được thay đổi, hãy tải lại lịch");
        }
        ensureEditable(showtime);
        if (bookingRepository.existsForShowtime(showtimeId)) {
            throw BusinessException.conflict("Không thể sửa suất chiếu đã có booking hoặc vé");
        }

        Room room = findActiveRoom(request.roomId());
        Slot slot = validateSlot(showtime.getMovie(), room, request);
        ensureNoDatabaseConflict(slot, showtimeId);
        showtime.setRoom(room);
        showtime.setStartsAt(slot.startsAt());
        showtime.setEndsAt(slot.endsAt());
        return AdminShowtimeResponse.from(saveSchedule(showtime), false);
    }

    @Override
    public void cancelShowtime(Long showtimeId) {
        Showtime observed = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy suất chiếu"));
        lockRooms(List.of(observed.getRoom().getId()));
        Showtime showtime = showtimeRepository.findByIdForUpdate(showtimeId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy suất chiếu"));
        if ("CANCELLED".equals(showtime.getStatus())) return;
        ensureEditable(showtime);
        if (bookingRepository.existsForShowtime(showtimeId)) {
            throw BusinessException.conflict("Không thể hủy suất chiếu đã có booking hoặc vé");
        }
        showtime.setStatus("CANCELLED");
        saveSchedule(showtime);
    }

    private List<Showtime> createSchedules(Movie movie, List<ShowtimeSlotRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();
        List<ShowtimeSlotRequest> slots = List.copyOf(requests);
        List<Long> roomIds = slots.stream().map(ShowtimeSlotRequest::roomId).distinct().sorted().toList();
        Map<Long, Room> lockedRooms = lockRooms(roomIds);

        List<Slot> prepared = new ArrayList<>(slots.size());
        for (ShowtimeSlotRequest request : slots) {
            Room room = lockedRooms.get(request.roomId());
            if (room == null || !"ACTIVE".equals(room.getStatus())) {
                throw BusinessException.badRequest("Phòng chiếu không tồn tại hoặc đang ngừng hoạt động");
            }
            prepared.add(validateSlot(movie, room, request));
        }

        ensureNoBatchConflicts(prepared);
        for (Slot slot : prepared) ensureNoDatabaseConflict(slot, (Long) null);

        List<Showtime> entities = prepared.stream().map(slot -> {
            Showtime showtime = new Showtime();
            showtime.setMovie(movie);
            showtime.setRoom(slot.room());
            showtime.setStartsAt(slot.startsAt());
            showtime.setEndsAt(slot.endsAt());
            showtime.setStatus("SCHEDULED");
            return showtime;
        }).toList();
        try {
            return showtimeRepository.saveAllAndFlush(entities);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Phòng đã có suất chiếu trùng hoặc không đủ 15 phút chuẩn bị");
        }
    }

    private Slot validateSlot(Movie movie, Room room, ShowtimeSlotRequest request) {
        if (request == null || request.startsAt() == null || request.roomId() == null) {
            throw BusinessException.badRequest("Thiếu phòng hoặc giờ bắt đầu suất chiếu");
        }
        Instant startsAt = request.startsAt().toInstant();
        if (!startsAt.isAfter(Instant.now())) {
            throw BusinessException.badRequest("Không thể tạo suất chiếu trong quá khứ");
        }
        LocalDateTime localStart = startsAt.atZone(CINEMA_ZONE).toLocalDateTime();
        LocalTime startTime = localStart.toLocalTime();
        if (startTime.isBefore(OPENING_TIME)
                || startTime.getMinute() % 15 != 0
                || startTime.getSecond() != 0
                || startTime.getNano() != 0) {
            throw BusinessException.badRequest("Giờ bắt đầu phải nằm trong khung 08:00–23:00 và theo bước 15 phút");
        }

        Instant endsAt = startsAt.plus(Duration.ofMinutes(movie.getDurationMinutes()));
        ensureEndInsideBusinessHours(startsAt, endsAt);
        return new Slot(room, startsAt, endsAt);
    }

    private void ensureNoBatchConflicts(List<Slot> slots) {
        List<Slot> sorted = slots.stream()
                .sorted(Comparator.comparing((Slot slot) -> slot.room().getId())
                        .thenComparing(Slot::startsAt))
                .toList();
        Map<Long, Instant> lastEndByRoom = new HashMap<>();
        for (Slot slot : sorted) {
            Instant lastEnd = lastEndByRoom.get(slot.room().getId());
            if (lastEnd != null && slot.startsAt().isBefore(lastEnd.plus(ROOM_BUFFER))) {
                throw BusinessException.conflict("Hai suất trong cùng phòng cần cách nhau ít nhất 15 phút");
            }
            lastEndByRoom.put(slot.room().getId(), slot.endsAt());
        }
    }

    private void ensureNoDatabaseConflict(Slot candidate, Long excludedShowtimeId) {
        ensureNoDatabaseConflict(candidate,
                excludedShowtimeId == null ? Set.of() : Set.of(excludedShowtimeId));
    }

    private void ensureNoDatabaseConflict(Slot candidate, Set<Long> excludedShowtimeIds) {
        Instant startWithBuffer = candidate.startsAt().minus(ROOM_BUFFER);
        Instant endWithBuffer = candidate.endsAt().plus(ROOM_BUFFER);
        boolean conflict = showtimeRepository.findRoomConflicts(
                        candidate.room().getId(), startWithBuffer, endWithBuffer)
                .stream()
                .anyMatch(existing -> !excludedShowtimeIds.contains(existing.getId()));
        if (conflict) {
            throw BusinessException.conflict("Phòng đã có suất chiếu trùng hoặc không đủ 15 phút chuẩn bị");
        }
    }

    private void ensureEndInsideBusinessHours(Instant startsAt, Instant endsAt) {
        LocalDateTime localStart = startsAt.atZone(CINEMA_ZONE).toLocalDateTime();
        LocalDateTime localEnd = endsAt.atZone(CINEMA_ZONE).toLocalDateTime();
        if (!localEnd.toLocalDate().equals(localStart.toLocalDate())
                || localEnd.toLocalTime().isAfter(CLOSING_TIME)) {
            throw BusinessException.badRequest("Suất chiếu phải kết thúc trước hoặc đúng 23:00 cùng ngày");
        }
    }

    private Map<Long, Room> lockRooms(Collection<Long> roomIds) {
        Map<Long, Room> rooms = new HashMap<>();
        roomIds.stream().distinct().sorted().forEach(roomId -> {
            Room room = roomRepository.findByIdForUpdate(roomId)
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng chiếu"));
            rooms.put(roomId, room);
        });
        return rooms;
    }

    private Room findActiveRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng chiếu"));
        if (!"ACTIVE".equals(room.getStatus())) {
            throw BusinessException.badRequest("Phòng chiếu đang ngừng hoạt động");
        }
        return room;
    }

    private void ensureEditable(Showtime showtime) {
        if (!"SCHEDULED".equals(showtime.getStatus())) {
            throw BusinessException.conflict("Chỉ có thể sửa hoặc hủy suất đang ở trạng thái SCHEDULED");
        }
    }

    private Showtime saveSchedule(Showtime showtime) {
        try {
            return showtimeRepository.saveAndFlush(showtime);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Phòng đã có suất chiếu trùng hoặc không đủ 15 phút chuẩn bị");
        }
    }

    private record Slot(Room room, Instant startsAt, Instant endsAt) {}
}
