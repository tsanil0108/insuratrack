package com.insuraTrack.repository;

import com.insuraTrack.model.InsuranceType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceTypeRepository extends JpaRepository<InsuranceType, String> {

    // Get all active (not deleted) insurance types
    List<InsuranceType> findAllByDeletedFalseAndActiveTrue();

    // Get all non-deleted insurance types
    List<InsuranceType> findAllByDeletedFalse();

    // Get all deleted insurance types
    List<InsuranceType> findAllByDeletedTrue();

    // Find by name (case insensitive) where not deleted
    Optional<InsuranceType> findByNameIgnoreCaseAndDeletedFalse(String name);

    // ✅ ADD THIS: Find by name (case insensitive) where deleted = true
    Optional<InsuranceType> findByNameIgnoreCaseAndDeletedTrue(String name);

    // Check if exists by name (case insensitive) where not deleted
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    // Check if exists by name (case insensitive) where deleted = true
    boolean existsByNameIgnoreCaseAndDeletedTrue(String name);

    // Search by name containing keyword
    List<InsuranceType> findByNameContainingIgnoreCaseAndDeletedFalse(String keyword);

    // Count active insurance types
    long countByDeletedFalseAndActiveTrue();

    // Restore a soft-deleted insurance type
    @Modifying
    @Transactional
    @Query("UPDATE InsuranceType it SET it.deleted = false, it.active = true, it.deletedBy = null, it.deletedAt = null, it.updatedAt = CURRENT_TIMESTAMP WHERE it.id = :id")
    void restoreById(@Param("id") String id);

}