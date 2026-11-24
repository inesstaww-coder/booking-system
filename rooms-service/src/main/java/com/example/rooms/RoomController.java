package com.example.rooms;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    public record RoomIn(String name, Integer capacity) {}
    public record Room(int id, String name, int capacity) {}

    private static final RowMapper<Room> MAPPER =
            (rs, i) -> new Room(rs.getInt("id"), rs.getString("name"), rs.getInt("capacity"));

    private final JdbcTemplate jdbc;

    public RoomController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Room> all() {
        return jdbc.query("SELECT id, name, capacity FROM rooms ORDER BY id", MAPPER);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> one(@PathVariable int id) {
        List<Room> list = jdbc.query("SELECT id, name, capacity FROM rooms WHERE id = ?", MAPPER, id);
        if (list.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Room not found"));
        return ResponseEntity.ok(list.get(0));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RoomIn body) {
        if (body.name() == null || body.name().isBlank() || body.capacity() == null || body.capacity() <= 0)
            return ResponseEntity.badRequest().body(Map.of("error", "name и capacity (>0) обязательны"));
        Integer id = jdbc.queryForObject(
                "INSERT INTO rooms(name, capacity) VALUES (?, ?) RETURNING id",
                Integer.class, body.name(), body.capacity());
        return ResponseEntity.status(201).body(new Room(id, body.name(), body.capacity()));
    }
}
