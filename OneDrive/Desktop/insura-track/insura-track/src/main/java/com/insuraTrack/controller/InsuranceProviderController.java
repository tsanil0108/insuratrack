package com.insuraTrack.controller;

import com.insuraTrack.model.InsuranceProvider;
import com.insuraTrack.repository.InsuranceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class InsuranceProviderController {

    private final InsuranceProviderRepository repo;

    @GetMapping
    public List<InsuranceProvider> getAll() {
        return repo.findByActiveTrue();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceProvider> create(@RequestBody InsuranceProvider provider) {
        return ResponseEntity.ok(repo.save(provider));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }
}