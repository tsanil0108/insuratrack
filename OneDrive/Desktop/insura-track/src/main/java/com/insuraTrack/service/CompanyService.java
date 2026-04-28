package com.insuraTrack.service;

import com.insuraTrack.model.Company;
import java.util.List;

public interface CompanyService {
    Company createCompany(Company company);
    Company getCompanyById(String id);
    List<Company> getAllCompanies();
    Company updateCompany(String id, Company updated);
    void softDeleteCompany(String id, String deletedByEmail);
}