package com.insuraTrack.service;

import com.insuraTrack.model.InsuranceProvider;
import java.util.List;

public interface InsuranceProviderService {
    InsuranceProvider create(InsuranceProvider provider);
    InsuranceProvider getById(String id);
    List<InsuranceProvider> getAll();
    InsuranceProvider update(String id, InsuranceProvider updated);
    void softDelete(String id, String deletedByEmail);
    InsuranceProvider restore(String id);  // ✅ Make sure this exists
    List<InsuranceProvider> getDeletedProviders();
    void permanentDelete(String id);
}