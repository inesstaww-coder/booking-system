package com.example.users;

import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    public record UserIn(String name, String email) {}
    public record User(int id, String name, String email) {}

    private static final RowMapper<User> MAPPER =
            (rs, i) -> new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"));

    private final JdbcTemplate jdbc;

    public UserController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<User> all() {
        return jdbc.query("SELECT id, name, email FROM users ORDER BY id", MAPPER);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> one(@PathVariable int id) {
        List<User> list = jdbc.query("SELECT id, name, email FROM users WHERE id = ?", MAPPER, id);
        if (list.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        return ResponseEntity.ok(list.get(0));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserIn body) {
        if (body.name() == null || body.name().isBlank() || body.email() == null || body.email().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "name и email обязательны"));
        try {
            Integer id = jdbc.queryForObject(
                    "INSERT INTO users(name, email) VALUES (?, ?) RETURNING id",
                    Integer.class, body.name(), body.email());
            return ResponseEntity.status(201).body(new User(id, body.name(), body.email()));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(409).body(Map.of("error", "Пользователь с таким email уже есть"));
        }
    }
}
