package com.insuraTrack.repository;

import com.insuraTrack.model.InsuranceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InsuranceProviderRepository extends JpaRepository<InsuranceProvider, String> {

    List<InsuranceProvider> findByActiveTrueAndDeletedFalse();

    default List<InsuranceProvider> findByActiveTrue() {
        return findByActiveTrueAndDeletedFalse();
    }

    // Soft delete support
    @Query("SELECT p FROM InsuranceProvider p WHERE p.deleted = true")
    List<InsuranceProvider> findAllDeleted();

    @Query("SELECT p FROM InsuranceProvider p WHERE p.id = :id AND p.deleted = true")
    Optional<InsuranceProvider> findDeletedById(@Param("id") String id);
}