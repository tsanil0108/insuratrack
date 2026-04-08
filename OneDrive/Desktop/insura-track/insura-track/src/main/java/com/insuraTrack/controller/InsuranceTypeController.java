package com.insuraTrack.controller;

import com.insuraTrack.model.InsuranceType;
import com.insuraTrack.repository.InsuranceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/insurance-types")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InsuranceTypeController {

    private final InsuranceTypeRepository repo;

    // Get all active insurance types
    @GetMapping
    public ResponseEntity<List<InsuranceType>> getAll() {
        List<InsuranceType> types = repo.findByActiveTrue();
        return ResponseEntity.ok(types);
    }

    // Get all insurance types (including inactive) - Admin only
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InsuranceType>> getAllIncludingInactive() {
        List<InsuranceType> types = repo.findAll();
        return ResponseEntity.ok(types);
    }

    // Get insurance type by ID
    @GetMapping("/{id}")
    public ResponseEntity<InsuranceType> getById(@PathVariable String id) {
        Optional<InsuranceType> type = repo.findById(id);
        return type.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new insurance type (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceType> create(@RequestBody InsuranceType type) {
        // Validation
        if (type.getName() == null || type.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Check if name already exists
        Optional<InsuranceType> existing = repo.findByNameIgnoreCase(type.getName());
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        type.setActive(true);
        InsuranceType saved = repo.save(type);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Update insurance type (Admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceType> update(@PathVariable String id,
                                                @RequestBody InsuranceType type) {
        Optional<InsuranceType> existing = repo.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InsuranceType updated = existing.get();
        updated.setName(type.getName());
        updated.setDescription(type.getDescription());
        updated.setActive(type.isActive());

        return ResponseEntity.ok(repo.save(updated));
    }

    // Soft delete insurance type (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Optional<InsuranceType> existing = repo.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Soft delete - just set active to false
        InsuranceType type = existing.get();
        type.setActive(false);
        repo.save(type);

        return ResponseEntity.ok().build();
    }

    // Hard delete (Permanent) - Admin only
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hardDelete(@PathVariable String id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }
}