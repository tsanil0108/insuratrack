package com.insuraTrack.service;

import com.insuraTrack.model.Company;
import com.insuraTrack.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public Company create(Company company) {
        return companyRepository.save(company);
    }

    public List<Company> getAll() {
        return companyRepository.findByActiveTrueAndDeletedFalse();
    }

    public Company getById(String id) {
        return companyRepository.findById(id)
                .filter(company -> !company.isDeleted())
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }

    public Company update(String id, Company updated) {
        Company company = getById(id);
        company.setName(updated.getName());
        company.setShortName(updated.getShortName());
        company.setAddress(updated.getAddress());
        company.setContactEmail(updated.getContactEmail());
        company.setActive(updated.isActive());
        return companyRepository.save(company);
    }

    // ✅ FIXED DELETE METHOD - Soft Delete
    @Transactional
    public void delete(String id) {
        Company company = companyRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new RuntimeException("Company not found or already deleted"));

        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        company.softDelete(currentUser);
        companyRepository.save(company);

        System.out.println("✅ Company soft deleted: " + id + " by " + currentUser);
    }
}