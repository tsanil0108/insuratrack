package com.insuraTrack.repository;

import com.insuraTrack.enums.PolicyStatus;
import com.insuraTrack.model.Policy;
import com.insuraTrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, String> {

    @Query("""
        SELECT p FROM Policy p
        JOIN FETCH p.company
        JOIN FETCH p.insuranceType
        JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
        WHERE p.deleted = false
    """)
    List<Policy> findAllActive();

    @Query("""
        SELECT p FROM Policy p
        JOIN FETCH p.company
        JOIN FETCH p.insuranceType
        JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
        WHERE p.user = :user AND p.deleted = false
    """)
    List<Policy> findByUserAndDeletedFalse(@Param("user") User user);

    @Query("""
        SELECT p FROM Policy p
        JOIN FETCH p.company
        JOIN FETCH p.insuranceType
        JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
        WHERE p.company.id = :companyId AND p.deleted = false
    """)
    List<Policy> findByCompanyIdAndDeletedFalse(@Param("companyId") String companyId);

    @Query("""
        SELECT p FROM Policy p
        JOIN FETCH p.company
        JOIN FETCH p.insuranceType
        JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
        WHERE p.status = :status AND p.deleted = false
    """)
    List<Policy> findByStatusAndDeletedFalse(@Param("status") PolicyStatus status);

    @Query("""
        SELECT p FROM Policy p
        JOIN FETCH p.company
        JOIN FETCH p.insuranceType
        JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
        WHERE p.endDate BETWEEN :today AND :soon AND p.deleted = false
    """)
    List<Policy> findExpiringSoon(@Param("today") LocalDate today,
                                  @Param("soon") LocalDate soon);

    @Query("""
        SELECT p FROM Policy p
        JOIN FETCH p.company
        JOIN FETCH p.insuranceType
        JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
        WHERE p.endDate < :today AND p.status = :status AND p.deleted = false
    """)
    List<Policy> findExpired(@Param("today") LocalDate today,
                             @Param("status") PolicyStatus status);

    @Query("SELECT COUNT(p) FROM Policy p WHERE p.status = :status AND p.deleted = false")
    long countByStatus(@Param("status") PolicyStatus status);

    @Query("SELECT COALESCE(SUM(p.premiumAmount), 0) FROM Policy p WHERE p.deleted = false")
    Double sumAllPremiums();

    @Query("""
        SELECT p FROM Policy p
        JOIN FETCH p.company
        JOIN FETCH p.insuranceType
        JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
        WHERE p.id = :id AND p.deleted = false
    """)
    Optional<Policy> findActiveById(@Param("id") String id);

    // ─── SOFT DELETE SUPPORT ─────────────────────────────────────────────────

    @Query("""
        SELECT p FROM Policy p
        LEFT JOIN FETCH p.company
        LEFT JOIN FETCH p.insuranceType
        LEFT JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
        WHERE p.deleted = true
    """)
    List<Policy> findAllDeleted();

    @Query("""
        SELECT p FROM Policy p
        LEFT JOIN FETCH p.company
        LEFT JOIN FETCH p.insuranceType
        LEFT JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
        WHERE p.id = :id AND p.deleted = true
    """)
    Optional<Policy> findDeletedById(@Param("id") String id);

    @Query("""
        SELECT p FROM Policy p
        LEFT JOIN FETCH p.insuranceType
        LEFT JOIN FETCH p.company
        LEFT JOIN FETCH p.provider
        LEFT JOIN FETCH p.user
    """)
    List<Policy> findAllWithDetails();
}