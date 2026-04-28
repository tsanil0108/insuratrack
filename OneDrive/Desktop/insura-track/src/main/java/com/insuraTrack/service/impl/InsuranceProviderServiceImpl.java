package com.insuraTrack.service.impl;

import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.InsuranceProvider;
import com.insuraTrack.repository.InsuranceProviderRepository;
import com.insuraTrack.service.InsuranceProviderService;
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
public class InsuranceProviderServiceImpl implements InsuranceProviderService {

    private final InsuranceProviderRepository providerRepository;

    @Override
    @Transactional
    public InsuranceProvider create(InsuranceProvider provider) {
        // Generate ID if not present
        if (provider.getId() == null || provider.getId().isBlank()) {
            provider.setId(UUID.randomUUID().toString());
        }

        // Validate phone number if provided
        if (provider.getPhoneNumber() != null && !provider.getPhoneNumber().isBlank()) {
            String phone = provider.getPhoneNumber().trim();
            if (!phone.matches("\\d{10}")) {
                throw new IllegalArgumentException("Mobile number must be exactly 10 digits");
            }
            provider.setPhoneNumber(phone);
        }

        // Check if name already exists (must be unique) - ONLY FOR CREATE
        if (providerRepository.existsByNameAndDeletedFalse(provider.getName())) {
            throw new IllegalArgumentException(
                    "Provider with name '" + provider.getName() + "' already exists!"
            );
        }

        // Check if a DELETED provider exists with same name
        java.util.Optional<InsuranceProvider> deletedByName = providerRepository.findByNameAndDeletedTrue(provider.getName());

        // If a deleted provider exists with same name, restore it
        if (deletedByName.isPresent()) {
            InsuranceProvider existingDeleted = deletedByName.get();
            log.info("Found deleted provider with same name. Restoring instead of creating new. ID: {}", existingDeleted.getId());

            // Update the existing deleted provider with new data
            existingDeleted.setName(provider.getName());
            existingDeleted.setContactInfo(provider.getContactInfo());
            existingDeleted.setEmail(provider.getEmail());
            existingDeleted.setPhoneNumber(provider.getPhoneNumber());
            existingDeleted.setActive(true);
            existingDeleted.setDeleted(false);
            existingDeleted.setDeletedAt(null);
            existingDeleted.setDeletedBy(null);
            existingDeleted.setUpdatedAt(LocalDateTime.now());

            return providerRepository.save(existingDeleted);
        }

        // Create fresh provider
        provider.setDeleted(false);
        provider.setActive(true);
        provider.setDeletedAt(null);
        provider.setDeletedBy(null);
        provider.setCreatedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());

        log.info("Creating new provider: {}", provider.getName());
        return providerRepository.save(provider);
    }

    @Override
    public InsuranceProvider getById(String id) {
        return providerRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Active provider not found with id: " + id));
    }

    @Override
    public List<InsuranceProvider> getAll() {
        return providerRepository.findAllByDeletedFalseAndActiveTrue();
    }

    @Override
    @Transactional
    public InsuranceProvider update(String id, InsuranceProvider updated) {
        InsuranceProvider existing = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id));

        if (existing.isDeleted()) {
            throw new IllegalStateException("Cannot update deleted provider! Restore it first from Recycle Bin.");
        }

        // Validate phone number if provided
        if (updated.getPhoneNumber() != null && !updated.getPhoneNumber().isBlank()) {
            String phone = updated.getPhoneNumber().trim();
            if (!phone.matches("\\d{10}")) {
                throw new IllegalArgumentException("Mobile number must be exactly 10 digits");
            }
            updated.setPhoneNumber(phone);
        }

        // ✅ FIX: Check name uniqueness (excluding current provider)
        // Only check if the name is changing AND another active provider (not this one) has the same name
        if (!existing.getName().equals(updated.getName())) {
            if (providerRepository.existsByNameAndDeletedFalse(updated.getName())) {
                throw new IllegalArgumentException("Provider with name '" + updated.getName() + "' already exists!");
            }
        }

        existing.setName(updated.getName());
        existing.setContactInfo(updated.getContactInfo());
        existing.setEmail(updated.getEmail());
        existing.setPhoneNumber(updated.getPhoneNumber());
        existing.setActive(updated.isActive());
        existing.setUpdatedAt(LocalDateTime.now());

        log.info("Updating provider: {}", existing.getName());
        return providerRepository.save(existing);
    }

    @Override
    @Transactional
    public void softDelete(String id, String deletedByEmail) {
        InsuranceProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id));

        if (provider.isDeleted()) {
            throw new IllegalStateException("Provider is already deleted!");
        }

        // Soft delete - Move to Recycle Bin
        provider.setDeleted(true);
        provider.setDeletedBy(deletedByEmail);
        provider.setDeletedAt(LocalDateTime.now());
        provider.setActive(false);
        provider.setUpdatedAt(LocalDateTime.now());

        log.info("Soft deleted provider: {} by {}", provider.getName(), deletedByEmail);
        providerRepository.save(provider);
    }

    @Override
    @Transactional
    public InsuranceProvider restore(String id) {
        InsuranceProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id));

        if (!provider.isDeleted()) {
            throw new IllegalStateException("Provider is not deleted! Cannot restore.");
        }

        // Check if an ACTIVE provider exists with same name (during restore)
        if (providerRepository.existsByNameAndDeletedFalse(provider.getName())) {
            throw new IllegalArgumentException(
                    "Cannot restore! A provider with name '" + provider.getName() + "' already exists!"
            );
        }

        provider.setDeleted(false);
        provider.setActive(true);
        provider.setDeletedAt(null);
        provider.setDeletedBy(null);
        provider.setUpdatedAt(LocalDateTime.now());

        log.info("Restored provider: {}", provider.getName());
        return providerRepository.save(provider);
    }

    @Override
    public List<InsuranceProvider> getDeletedProviders() {
        return providerRepository.findAllByDeletedTrue();
    }

    @Override
    @Transactional
    public void permanentDelete(String id) {
        InsuranceProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id));

        if (!provider.isDeleted()) {
            throw new IllegalStateException("Soft delete first before permanent deletion!");
        }

        log.warn("Permanently deleting provider: {}", provider.getName());
        providerRepository.delete(provider);
    }
}