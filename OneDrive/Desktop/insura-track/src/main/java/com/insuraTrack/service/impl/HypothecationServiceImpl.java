package com.insuraTrack.service.impl;

import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.Hypothecation;
import com.insuraTrack.repository.HypothecationRepository;
import com.insuraTrack.service.HypothecationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HypothecationServiceImpl implements HypothecationService {

    private final HypothecationRepository hypothecationRepository;

    @Override
    @Transactional
    public Hypothecation create(Hypothecation hypothecation) {
        return hypothecationRepository.save(hypothecation);
    }

    @Override
    public Hypothecation getById(String id) {
        return hypothecationRepository.findById(id)
                .filter(h -> !h.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Hypothecation not found: " + id));
    }

    @Override
    public List<Hypothecation> getAll() {
        return hypothecationRepository.findAllByDeletedFalseAndActiveTrue();
    }

    @Override
    @Transactional
    public Hypothecation update(String id, Hypothecation updated) {
        Hypothecation existing = getById(id);
        existing.setBankName(updated.getBankName());
        existing.setEmployeeName(updated.getEmployeeName());
        existing.setMobileNumber(updated.getMobileNumber());
        existing.setEmail(updated.getEmail());
        existing.setActive(updated.isActive());
        return hypothecationRepository.save(existing);
    }

    @Override
    @Transactional
    public void softDelete(String id, String deletedByEmail) {
        Hypothecation h = getById(id);
        h.softDelete(deletedByEmail);
        hypothecationRepository.save(h);
    }
}