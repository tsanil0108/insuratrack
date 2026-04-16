package com.insuraTrack.repository;

import com.insuraTrack.model.InsuranceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceTypeRepository extends JpaRepository<InsuranceType, String> {

    List<InsuranceType> findByActiveTrue();

    Optional<InsuranceType> findByNameIgnoreCase(String name);

    List<InsuranceType> findByNameContainingIgnoreCase(String name);

    // ─── Soft delete support ──────────────────────────────────────────────

    @Query("SELECT t FROM InsuranceType t WHERE t.deleted = false")
    List<InsuranceType> findAllActive();

    @Query("SELECT t FROM InsuranceType t WHERE t.deleted = true")
    List<InsuranceType> findAllDeleted();

    @Query("SELECT t FROM InsuranceType t WHERE t.id = :id AND t.deleted = true")
    Optional<InsuranceType> findDeletedById(@Param("id") String id);
}