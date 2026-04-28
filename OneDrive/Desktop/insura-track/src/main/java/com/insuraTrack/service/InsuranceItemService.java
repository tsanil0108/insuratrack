package com.insuraTrack.service;

import com.insuraTrack.model.InsuranceItem;
import java.util.List;

public interface InsuranceItemService {
    InsuranceItem create(InsuranceItem item);
    InsuranceItem getById(String id);
    List<InsuranceItem> getAll();
    List<InsuranceItem> getByInsuranceType(String insuranceTypeId);
    InsuranceItem update(String id, InsuranceItem updated);
    void softDelete(String id, String deletedByEmail);
}