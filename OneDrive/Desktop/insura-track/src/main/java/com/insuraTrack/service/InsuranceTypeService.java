package com.insuraTrack.service;

import com.insuraTrack.model.InsuranceType;
import com.insuraTrack.repository.InsuranceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InsuranceTypeService {

    private final InsuranceTypeRepository typeRepository;

    public List<InsuranceType> getAll() {
        return typeRepository.findAllActive(); // This already filters deleted
    }

    // ✅ FIX - Only fetch non-deleted
    public InsuranceType getById(String id) {
        return typeRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new RuntimeException("Insurance Type not found"));
    }

    // ✅ FIX DELETE METHOD
    @Transactional
    public void delete(String id) {
        InsuranceType type = typeRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new RuntimeException("Insurance Type not found or already deleted"));

        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        type.softDelete(currentUser);
        typeRepository.save(type);

        System.out.println("✅ Insurance Type deleted: " + id + " by " + currentUser);
    }
}