package com.insuraTrack.repository;

import com.insuraTrack.model.InsuranceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceProviderRepository extends JpaRepository<InsuranceProvider, String> {

    // Active providers (not deleted)
    List<InsuranceProvider> findAllByDeletedFalseAndActiveTrue();

    // All non-deleted providers
    List<InsuranceProvider> findAllByDeletedFalse();

    // Deleted providers only
    List<InsuranceProvider> findAllByDeletedTrue();

    // Check if name exists (only non-deleted)
    boolean existsByNameAndDeletedFalse(String name);

    // Find deleted provider by name
    Optional<InsuranceProvider> findByNameAndDeletedTrue(String name);

    // Find deleted provider by email
    Optional<InsuranceProvider> findByEmailAndDeletedTrue(String email);

    // Find by ID
    Optional<InsuranceProvider> findById(String id);

    // Search in deleted providers
    @Query("SELECT p FROM InsuranceProvider p WHERE p.deleted = true AND (p.name LIKE %:keyword% OR p.email LIKE %:keyword% OR p.phoneNumber LIKE %:keyword%)")
    List<InsuranceProvider> searchDeletedProviders(@Param("keyword") String keyword);

    // ✅ Keep these for update uniqueness check
    @Query("SELECT COUNT(p) > 0 FROM InsuranceProvider p WHERE p.deleted = false AND p.name = :name AND p.id != :id")
    boolean existsByNameAndDeletedFalseAndIdNot(@Param("name") String name, @Param("id") String id);
}