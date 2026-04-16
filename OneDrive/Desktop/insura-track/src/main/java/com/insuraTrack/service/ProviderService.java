package com.insuraTrack.service;

import com.insuraTrack.model.InsuranceProvider;
import com.insuraTrack.repository.InsuranceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final InsuranceProviderRepository providerRepository;

    public InsuranceProvider create(InsuranceProvider provider) {
        return providerRepository.save(provider);
    }

    public List<InsuranceProvider> getAll() {
        return providerRepository.findByActiveTrueAndDeletedFalse();
    }

    public InsuranceProvider getById(String id) {
        return providerRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }

    // ✅ FIXED - Use actual model fields
    public InsuranceProvider update(String id, InsuranceProvider updated) {
        InsuranceProvider provider = getById(id);
        provider.setName(updated.getName());
        provider.setContactInfo(updated.getContactInfo());  // ← Changed from setContactPerson
        provider.setActive(updated.isActive());
        // Remove setCode, setPhone, setEmail as they don't exist in model
        return providerRepository.save(provider);
    }

    // ✅ SOFT DELETE
    @Transactional
    public void delete(String id) {
        InsuranceProvider provider = providerRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new RuntimeException("Provider not found or already deleted"));

        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        provider.softDelete(currentUser);
        providerRepository.save(provider);

        System.out.println("✅ Provider soft deleted: " + id + " by " + currentUser);
    }
}