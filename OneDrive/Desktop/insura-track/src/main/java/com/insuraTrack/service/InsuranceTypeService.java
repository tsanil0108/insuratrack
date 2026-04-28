package com.insuraTrack.service;

import com.insuraTrack.model.InsuranceType;
import java.util.List;

public interface InsuranceTypeService {
    InsuranceType create(InsuranceType insuranceType);
    InsuranceType getById(String id);
    List<InsuranceType> getAll();
    InsuranceType update(String id, InsuranceType updated);
    void softDelete(String id, String deletedByEmail);
}