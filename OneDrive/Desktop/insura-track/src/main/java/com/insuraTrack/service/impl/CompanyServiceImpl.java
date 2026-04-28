package com.insuraTrack.service.impl;

import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.Company;
import com.insuraTrack.repository.CompanyRepository;
import com.insuraTrack.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public Company createCompany(Company company) {
        // ✅ FIX: Allow same name if previously deleted (soft-deleted records don't conflict)
        // Only block if an ACTIVE (non-deleted) company with the same name exists
        boolean nameExistsActive = companyRepository
                .existsByNameIgnoreCaseAndDeletedFalse(company.getName().trim());
        if (nameExistsActive) {
            throw new IllegalArgumentException(
                    "A company with the name '" + company.getName() + "' already exists.");
        }
        company.setName(company.getName().trim());
        return companyRepository.save(company);
    }

    @Override
    public Company getCompanyById(String id) {
        return companyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
    }

    @Override
    public List<Company> getAllCompanies() {
        return companyRepository.findAllByDeletedFalseAndActiveTrue();
    }

    @Override
    @Transactional
    public Company updateCompany(String id, Company updated) {
        Company existing = getCompanyById(id);

        // ✅ FIX: Check for duplicate name only if name is being changed
        String newName = updated.getName().trim();
        if (!existing.getName().equalsIgnoreCase(newName)) {
            boolean nameExistsActive = companyRepository
                    .existsByNameIgnoreCaseAndDeletedFalse(newName);
            if (nameExistsActive) {
                throw new IllegalArgumentException(
                        "A company with the name '" + newName + "' already exists.");
            }
        }

        existing.setName(newName);
        existing.setShortName(updated.getShortName());
        existing.setAddress(updated.getAddress());
        existing.setCity(updated.getCity());
        existing.setDistrict(updated.getDistrict());
        existing.setState(updated.getState());
        existing.setPinCode(updated.getPinCode());
        existing.setContactEmail(updated.getContactEmail());
        existing.setContactPhone(updated.getContactPhone());
        existing.setActive(updated.isActive());
        return companyRepository.save(existing);
    }

    @Override
    @Transactional
    public void softDeleteCompany(String id, String deletedByEmail) {
        Company company = getCompanyById(id);
        company.softDelete(deletedByEmail);
        companyRepository.save(company);
    }
}