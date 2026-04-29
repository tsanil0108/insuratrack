package com.insuraTrack.service;

import com.insuraTrack.model.InsuranceItem;
import java.util.List;

public interface InsuranceItemService {
    InsuranceItem create(InsuranceItem item);
    InsuranceItem getById(String id);

    // Used in dropdowns / policy forms — active only
    List<InsuranceItem> getAll();

    // Used for analytics/graphs — active + inactive, non-deleted, LEFT JOIN FETCH insuranceType
    List<InsuranceItem> getAllIncludingInactive();

    List<InsuranceItem> getByInsuranceType(String insuranceTypeId);
    InsuranceItem update(String id, InsuranceItem updated);
    void softDelete(String id, String deletedByEmail);
}