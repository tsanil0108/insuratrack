package com.insuraTrack.controller;

import com.insuraTrack.model.Hypothecation;
import com.insuraTrack.service.HypothecationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hypothecations")
@RequiredArgsConstructor
public class HypothecationController {

    private final HypothecationService hypothecationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Hypothecation> create(@RequestBody Hypothecation hypothecation) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hypothecationService.create(hypothecation));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<Hypothecation>> getAll() {
        return ResponseEntity.ok(hypothecationService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Hypothecation> getById(@PathVariable String id) {
        return ResponseEntity.ok(hypothecationService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Hypothecation> update(@PathVariable String id, @RequestBody Hypothecation hypothecation) {
        return ResponseEntity.ok(hypothecationService.update(id, hypothecation));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id, @RequestParam String deletedBy) {
        hypothecationService.softDelete(id, deletedBy);
        return ResponseEntity.noContent().build();
    }
}