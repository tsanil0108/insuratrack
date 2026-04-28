package com.insuraTrack.controller;

import com.insuraTrack.model.InsuranceItem;
import com.insuraTrack.service.InsuranceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/insurance-items")
@RequiredArgsConstructor
public class InsuranceItemController {

    private final InsuranceItemService insuranceItemService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceItem> create(@RequestBody InsuranceItem item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insuranceItemService.create(item));
    }

    // ✅ SINGLE Get mapping with optional typeId parameter
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InsuranceItem>> getAll(
            @RequestParam(required = false) String typeId) {
        if (typeId != null && !typeId.isBlank()) {
            return ResponseEntity.ok(insuranceItemService.getByInsuranceType(typeId));
        }
        return ResponseEntity.ok(insuranceItemService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InsuranceItem> getById(@PathVariable String id) {
        return ResponseEntity.ok(insuranceItemService.getById(id));
    }

    // ✅ Keep this for backward compatibility or remove if not needed
    @GetMapping("/by-type/{insuranceTypeId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InsuranceItem>> getByInsuranceType(@PathVariable String insuranceTypeId) {
        return ResponseEntity.ok(insuranceItemService.getByInsuranceType(insuranceTypeId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceItem> update(@PathVariable String id, @RequestBody InsuranceItem item) {
        return ResponseEntity.ok(insuranceItemService.update(id, item));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id, @RequestParam String deletedBy) {
        insuranceItemService.softDelete(id, deletedBy);
        return ResponseEntity.noContent().build();
    }
}