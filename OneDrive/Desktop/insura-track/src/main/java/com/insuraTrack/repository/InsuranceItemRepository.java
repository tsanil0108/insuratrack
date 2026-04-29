package com.insuraTrack.repository;

import com.insuraTrack.model.InsuranceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceItemRepository extends JpaRepository<InsuranceItem, String> {

    // ✅ Only active items by type (used in policy forms)
    List<InsuranceItem> findAllByInsuranceTypeIdAndDeletedFalseAndActiveTrue(String insuranceTypeId);

    // ✅ Only active items (used in policy forms / dropdowns)
    List<InsuranceItem> findAllByDeletedFalseAndActiveTrue();

    // ✅ ALL non-deleted items including inactive (used for analytics/graphs)
    //    Uses simple derived query — safe for items that have a type
    List<InsuranceItem> findAllByDeletedFalse();

    // ✅ FIX: LEFT JOIN FETCH to eagerly load insuranceType safely,
    //         even for items where insurance_type_id IS NULL
    //         This prevents LazyInitializationException and N+1 queries
    @Query("SELECT ii FROM InsuranceItem ii LEFT JOIN FETCH ii.insuranceType WHERE ii.deleted = false")
    List<InsuranceItem> findAllByDeletedFalseWithType();

    // ✅ All deleted items (for recycle bin)
    @Query("SELECT ii FROM InsuranceItem ii WHERE ii.deleted = true")
    List<InsuranceItem> findAllByDeletedTrue();
}