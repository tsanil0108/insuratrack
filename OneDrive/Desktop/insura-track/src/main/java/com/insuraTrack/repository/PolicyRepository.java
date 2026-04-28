package com.insuraTrack.repository;

import com.insuraTrack.enums.PolicyStatus;
import com.insuraTrack.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {

    // Find by policy number (only non-deleted)
    Optional<Policy> findByPolicyNumberAndDeletedFalse(String policyNumber);

    // Check existence (only non-deleted)
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Policy p WHERE p.policyNumber = :policyNumber AND p.deleted = false")
    boolean existsByPolicyNumberAndDeletedFalse(@Param("policyNumber") String policyNumber);

    // Get all non-deleted policies
    @Query("SELECT p FROM Policy p WHERE p.deleted = false")
    List<Policy> findAllByDeletedFalse();

    // ✅ ADD THIS METHOD - Get all soft-deleted policies
    @Query("SELECT p FROM Policy p WHERE p.deleted = true")
    List<Policy> findAllByDeletedTrue();

    // Get by user (only non-deleted)
    @Query("SELECT p FROM Policy p WHERE p.user.id = :userId AND p.deleted = false")
    List<Policy> findAllByUserIdAndDeletedFalse(@Param("userId") String userId);

    // Get by status (only non-deleted)
    @Query("SELECT p FROM Policy p WHERE p.status = :status AND p.deleted = false")
    List<Policy> findAllByStatusAndDeletedFalse(@Param("status") PolicyStatus status);

    // Get by company (only non-deleted)
    @Query("SELECT p FROM Policy p WHERE p.company.id = :companyId AND p.deleted = false")
    List<Policy> findAllByCompanyIdAndDeletedFalse(@Param("companyId") String companyId);

    // Get by insurance type (only non-deleted)
    @Query("SELECT p FROM Policy p WHERE p.insuranceType.id = :insuranceTypeId AND p.deleted = false")
    List<Policy> findAllByInsuranceTypeIdAndDeletedFalse(@Param("insuranceTypeId") String insuranceTypeId);

    // Find by ID (only non-deleted)
    @Query("SELECT p FROM Policy p WHERE p.id = :id AND p.deleted = false")
    Optional<Policy> findByIdAndDeletedFalse(@Param("id") String id);

    // Find expiring policies
    @Query("SELECT p FROM Policy p WHERE p.deleted = false AND p.endDate BETWEEN :start AND :end")
    List<Policy> findExpiringBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // Find policies that were renewed from a specific policy
    @Query("SELECT p FROM Policy p WHERE p.renewedFromPolicyId = :policyId AND p.deleted = false")
    List<Policy> findByRenewedFromPolicyId(@Param("policyId") String policyId);

    // Find all policies expiring soon
    @Query("SELECT p FROM Policy p WHERE p.deleted = false AND p.status = 'EXPIRING_SOON'")
    List<Policy> findAllExpiringSoon();
}