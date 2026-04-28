package com.insuraTrack.repository;

import com.insuraTrack.model.InsuranceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceItemRepository extends JpaRepository<InsuranceItem, String> {

    List<InsuranceItem> findAllByInsuranceTypeIdAndDeletedFalseAndActiveTrue(String insuranceTypeId);

    List<InsuranceItem> findAllByDeletedFalseAndActiveTrue();

    // ✅ ADD THIS METHOD - For finding deleted insurance items
    @Query("SELECT ii FROM InsuranceItem ii WHERE ii.deleted = true")
    List<InsuranceItem> findAllByDeletedTrue();


}