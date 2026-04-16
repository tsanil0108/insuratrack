package com.insuraTrack.controller;

import com.insuraTrack.model.InsuranceProvider;
import com.insuraTrack.service.ProviderService;  // ← Create this service
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class InsuranceProviderController {

    private final ProviderService providerService;  // ← Use service, not repository

    @GetMapping
    public List<InsuranceProvider> getAll() {
        return providerService.getAll();
    }

    @GetMapping("/{id}")
    public InsuranceProvider getById(@PathVariable String id) {
        return providerService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceProvider> create(@RequestBody InsuranceProvider provider) {
        return ResponseEntity.ok(providerService.create(provider));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceProvider> update(@PathVariable String id,
                                                    @RequestBody InsuranceProvider provider) {
        return ResponseEntity.ok(providerService.update(id, provider));
    }

    // ✅ FIXED DELETE - Use service with soft delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        providerService.delete(id);
        return ResponseEntity.ok().build();
    }
}