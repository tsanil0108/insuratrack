package com.insuraTrack.controller;

import com.insuraTrack.dto.UserDTO;
import com.insuraTrack.model.User;
import com.insuraTrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── GET ALL USERS (Admin only) ──────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAll() {
        List<UserDTO> users = userRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ─── GET USER BY ID ──────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getById(@PathVariable String id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(toDTO(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── CREATE USER (Admin only) ────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String name     = body.get("name");
        String email    = body.get("email");
        String password = body.get("password");
        String role     = body.get("role");

        if (name == null || name.isBlank())     return ResponseEntity.badRequest().body("Name is required");
        if (email == null || email.isBlank())   return ResponseEntity.badRequest().body("Email is required");
        if (password == null || password.isBlank()) return ResponseEntity.badRequest().body("Password is required");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(409).body("Email already exists");
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role != null && role.equals("ADMIN")
                        ? com.insuraTrack.enums.Role.ADMIN
                        : com.insuraTrack.enums.Role.USER)
                .active(true)
                .build();

        return ResponseEntity.status(201).body(toDTO(userRepository.save(user)));
    }

    // ─── UPDATE USER (Admin only) ────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return userRepository.findById(id).map(user -> {
            if (body.containsKey("name") && !body.get("name").isBlank()) {
                user.setName(body.get("name"));
            }
            if (body.containsKey("email") && !body.get("email").isBlank()) {
                user.setEmail(body.get("email"));
            }
            if (body.containsKey("password") && !body.get("password").isBlank()) {
                user.setPassword(passwordEncoder.encode(body.get("password")));
            }
            if (body.containsKey("role")) {
                user.setRole(body.get("role").equals("ADMIN")
                        ? com.insuraTrack.enums.Role.ADMIN
                        : com.insuraTrack.enums.Role.USER);
            }
            if (body.containsKey("active")) {
                user.setActive(Boolean.parseBoolean(body.get("active")));
            }
            return ResponseEntity.ok(toDTO(userRepository.save(user)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── TOGGLE ACTIVE (Admin only) ──────────────────────────────────────────

    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> toggleActive(@PathVariable String id) {
        return userRepository.findById(id).map(user -> {
            user.setActive(!user.isActive());
            return ResponseEntity.ok(toDTO(userRepository.save(user)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── DELETE USER (Admin only) ────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!userRepository.existsById(id)) return ResponseEntity.notFound().build();
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ─── MAPPER ──────────────────────────────────────────────────────────────

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}