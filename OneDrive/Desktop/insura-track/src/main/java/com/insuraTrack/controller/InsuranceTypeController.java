package com.insuraTrack.controller;

import com.insuraTrack.model.InsuranceType;
import com.insuraTrack.service.InsuranceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/insurance-types")
@RequiredArgsConstructor
public class InsuranceTypeController {

    private final InsuranceTypeService insuranceTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceType> create(@RequestBody InsuranceType insuranceType) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insuranceTypeService.create(insuranceType));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InsuranceType>> getAll() {
        return ResponseEntity.ok(insuranceTypeService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InsuranceType> getById(@PathVariable String id) {
        return ResponseEntity.ok(insuranceTypeService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceType> update(@PathVariable String id, @RequestBody InsuranceType insuranceType) {
        return ResponseEntity.ok(insuranceTypeService.update(id, insuranceType));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id, @RequestParam String deletedBy) {
        insuranceTypeService.softDelete(id, deletedBy);
        return ResponseEntity.noContent().build();
    }
}