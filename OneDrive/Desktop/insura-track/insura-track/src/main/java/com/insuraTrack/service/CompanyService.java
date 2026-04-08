package com.insuraTrack.service;

import com.insuraTrack.model.Company;
import com.insuraTrack.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public Company create(Company company) {
        return companyRepository.save(company);
    }

    public List<Company> getAll() {
        return companyRepository.findByActiveTrue();
    }

    public Company getById(String id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }

    public Company update(String id, Company updated) {
        Company company = getById(id);
        company.setName(updated.getName());
        company.setShortName(updated.getShortName());
        company.setAddress(updated.getAddress());
        company.setContactEmail(updated.getContactEmail());
        return companyRepository.save(company);
    }

    // Soft delete — moves to recycle bin
    public void delete(String id) {
        Company company = getById(id);
        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        company.softDelete(currentUser);
        companyRepository.save(company);
    }
}