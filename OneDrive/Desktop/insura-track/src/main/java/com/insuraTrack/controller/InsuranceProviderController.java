package com.insuraTrack.controller;

import com.insuraTrack.model.InsuranceProvider;
import com.insuraTrack.service.InsuranceProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")  // ✅ CHANGED from /insurance-providers to /providers
@RequiredArgsConstructor
public class InsuranceProviderController {

    private final InsuranceProviderService providerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceProvider> create(@RequestBody InsuranceProvider provider) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.create(provider));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InsuranceProvider>> getAll() {
        return ResponseEntity.ok(providerService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InsuranceProvider> getById(@PathVariable String id) {
        return ResponseEntity.ok(providerService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceProvider> update(@PathVariable String id, @RequestBody InsuranceProvider provider) {
        return ResponseEntity.ok(providerService.update(id, provider));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id, @RequestParam String deletedBy) {
        providerService.softDelete(id, deletedBy);
        return ResponseEntity.noContent().build();
    }
}