package com.example.bookings;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    public record BookingIn(Integer roomId, Integer userId, OffsetDateTime startTime, OffsetDateTime endTime) {}
    public record Booking(int id, int roomId, String roomName, int userId, String userName,
                          Instant startTime, Instant endTime) {}
    public record RoomDto(int id, String name, int capacity) {}
    public record UserDto(int id, String name, String email) {}

    private static final RowMapper<Booking> MAPPER = (rs, i) -> new Booking(
            rs.getInt("id"), rs.getInt("room_id"), rs.getString("room_name"),
            rs.getInt("user_id"), rs.getString("user_name"),
            rs.getTimestamp("start_time").toInstant(), rs.getTimestamp("end_time").toInstant());

    private final JdbcTemplate jdbc;
    private final RestClient http = RestClient.create();
    private final String roomsUrl;
    private final String usersUrl;

    public BookingController(JdbcTemplate jdbc,
                             @Value("${rooms.url}") String roomsUrl,
                             @Value("${users.url}") String usersUrl) {
        this.jdbc = jdbc;
        this.roomsUrl = roomsUrl;
        this.usersUrl = usersUrl;
    }

    @GetMapping
    public List<Booking> all() {
        return jdbc.query("SELECT * FROM bookings ORDER BY id", MAPPER);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BookingIn body) {
        if (body.roomId() == null || body.userId() == null || body.startTime() == null || body.endTime() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "roomId, userId, startTime, endTime обязательны"));
        Instant start = body.startTime().toInstant();
        Instant end = body.endTime().toInstant();
        if (!end.isAfter(start))
            return ResponseEntity.badRequest().body(Map.of("error", "endTime должен быть позже startTime"));

        // Взаимодействие сервисов по HTTP
        RoomDto room;
        UserDto user;
        try {
            try {
                room = http.get().uri(roomsUrl + "/rooms/{id}", body.roomId()).retrieve().body(RoomDto.class);
            } catch (HttpClientErrorException.NotFound e) {
                return ResponseEntity.status(404).body(Map.of("error", "Комната не найдена (rooms-service)"));
            }
            try {
                user = http.get().uri(usersUrl + "/users/{id}", body.userId()).retrieve().body(UserDto.class);
            } catch (HttpClientErrorException.NotFound e) {
                return ResponseEntity.status(404).body(Map.of("error", "Пользователь не найден (users-service)"));
            }
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(503).body(Map.of("error", "Соседний сервис недоступен"));
        }

        Timestamp s = Timestamp.from(start);
        Timestamp e = Timestamp.from(end);
        Long busy = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE room_id = ? AND start_time < ? AND end_time > ?",
                Long.class, room.id(), e, s);
        if (busy != null && busy > 0)
            return ResponseEntity.status(409).body(Map.of("error", "Комната уже занята на это время"));

        Integer id = jdbc.queryForObject(
                "INSERT INTO bookings(room_id, room_name, user_id, user_name, start_time, end_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                Integer.class, room.id(), room.name(), user.id(), user.name(), s, e);
        return ResponseEntity.status(201)
                .body(new Booking(id, room.id(), room.name(), user.id(), user.name(), start, end));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        int n = jdbc.update("DELETE FROM bookings WHERE id = ?", id);
        return n > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
