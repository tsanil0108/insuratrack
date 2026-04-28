package com.insuraTrack.service.impl;

import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.InsuranceItem;
import com.insuraTrack.repository.InsuranceItemRepository;
import com.insuraTrack.service.InsuranceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceItemServiceImpl implements InsuranceItemService {

    private final InsuranceItemRepository insuranceItemRepository;

    @Override
    @Transactional
    public InsuranceItem create(InsuranceItem item) {
        return insuranceItemRepository.save(item);
    }

    @Override
    public InsuranceItem getById(String id) {
        return insuranceItemRepository.findById(id)
                .filter(i -> !i.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceItem not found: " + id));
    }

    @Override
    public List<InsuranceItem> getAll() {
        return insuranceItemRepository.findAllByDeletedFalseAndActiveTrue();
    }

    @Override
    public List<InsuranceItem> getByInsuranceType(String insuranceTypeId) {
        return insuranceItemRepository.findAllByInsuranceTypeIdAndDeletedFalseAndActiveTrue(insuranceTypeId);
    }

    @Override
    @Transactional
    public InsuranceItem update(String id, InsuranceItem updated) {
        InsuranceItem existing = getById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setActive(updated.isActive());
        existing.setInsuranceType(updated.getInsuranceType());
        return insuranceItemRepository.save(existing);
    }

    @Override
    @Transactional
    public void softDelete(String id, String deletedByEmail) {
        InsuranceItem item = getById(id);
        item.softDelete(deletedByEmail);
        insuranceItemRepository.save(item);
    }
}