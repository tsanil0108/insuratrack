package com.insuraTrack.service.impl;

import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.InsuranceType;
import com.insuraTrack.repository.InsuranceTypeRepository;
import com.insuraTrack.service.InsuranceTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceTypeServiceImpl implements InsuranceTypeService {

    private final InsuranceTypeRepository insuranceTypeRepository;

    @Override
    @Transactional
    public InsuranceType create(InsuranceType insuranceType) {
        // Generate ID if not present
        if (insuranceType.getId() == null || insuranceType.getId().isBlank()) {
            insuranceType.setId(UUID.randomUUID().toString());
        }

        // Check if an ACTIVE insurance type exists with same name
        if (insuranceTypeRepository.existsByNameIgnoreCaseAndDeletedFalse(insuranceType.getName())) {
            throw new IllegalArgumentException(
                    "An ACTIVE insurance type with name '" + insuranceType.getName() + "' already exists! Please use a different name."
            );
        }

        // Check if a DELETED insurance type exists with same name
        java.util.Optional<InsuranceType> deletedType = insuranceTypeRepository.findByNameIgnoreCaseAndDeletedTrue(insuranceType.getName());

        // If a deleted insurance type exists with same name, automatically restore it instead of creating new
        if (deletedType.isPresent()) {
            InsuranceType existingDeleted = deletedType.get();

            log.info("Found deleted insurance type with same name. Restoring instead of creating new. ID: {}", existingDeleted.getId());

            // Update the existing deleted insurance type with new data
            existingDeleted.setName(insuranceType.getName());
            existingDeleted.setDescription(insuranceType.getDescription());
            existingDeleted.setActive(true);
            existingDeleted.setDeleted(false);
            existingDeleted.setDeletedAt(null);
            existingDeleted.setDeletedBy(null);
            existingDeleted.setUpdatedAt(LocalDateTime.now());

            return insuranceTypeRepository.save(existingDeleted);
        }

        // Create fresh insurance type (no conflicts)
        insuranceType.setDeleted(false);
        insuranceType.setActive(true);
        insuranceType.setDeletedAt(null);
        insuranceType.setDeletedBy(null);
        insuranceType.setCreatedAt(LocalDateTime.now());
        insuranceType.setUpdatedAt(LocalDateTime.now());

        log.info("Creating new insurance type: {}", insuranceType.getName());
        return insuranceTypeRepository.save(insuranceType);
    }

    @Override
    public InsuranceType getById(String id) {
        return insuranceTypeRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceType not found: " + id));
    }

    @Override
    public List<InsuranceType> getAll() {
        return insuranceTypeRepository.findAllByDeletedFalseAndActiveTrue();
    }

    @Override
    @Transactional
    public InsuranceType update(String id, InsuranceType updated) {
        InsuranceType existing = getById(id);

        // Check if name is being changed and if new name already exists (excluding current)
        if (!existing.getName().equalsIgnoreCase(updated.getName()) &&
                insuranceTypeRepository.existsByNameIgnoreCaseAndDeletedFalse(updated.getName())) {
            throw new IllegalArgumentException(
                    "Insurance type with name '" + updated.getName() + "' already exists!"
            );
        }

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setActive(updated.isActive());
        existing.setUpdatedAt(LocalDateTime.now());

        log.info("Updated insurance type: {}", existing.getName());
        return insuranceTypeRepository.save(existing);
    }

    @Override
    @Transactional
    public void softDelete(String id, String deletedByEmail) {
        InsuranceType type = getById(id);
        type.softDelete(deletedByEmail);
        insuranceTypeRepository.save(type);
        log.info("Soft deleted insurance type: {} by {}", type.getName(), deletedByEmail);
    }
}