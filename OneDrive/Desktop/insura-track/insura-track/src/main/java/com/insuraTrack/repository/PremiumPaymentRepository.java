package com.insuraTrack.repository;

import com.insuraTrack.model.PremiumPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PremiumPaymentRepository extends JpaRepository<PremiumPayment, String> {

    @Query("""
        SELECT pp FROM PremiumPayment pp
        JOIN FETCH pp.policy p
        JOIN FETCH p.company
        JOIN FETCH p.provider
        WHERE pp.policy.id = :policyId AND pp.deleted = false
    """)
    List<PremiumPayment> findByPolicyId(@Param("policyId") String policyId);

    @Query("""
        SELECT pp FROM PremiumPayment pp
        JOIN FETCH pp.policy p
        JOIN FETCH p.company
        JOIN FETCH p.provider
        WHERE pp.deleted = false
    """)
    List<PremiumPayment> findAllActive();

    @Query("""
        SELECT pp FROM PremiumPayment pp
        JOIN FETCH pp.policy p
        JOIN FETCH p.company
        JOIN FETCH p.provider
        WHERE pp.status = 'UNPAID' AND pp.dueDate < :today AND pp.deleted = false
    """)
    List<PremiumPayment> findOverdue(@Param("today") LocalDate today);

    @Query("""
        SELECT pp FROM PremiumPayment pp
        JOIN FETCH pp.policy p
        JOIN FETCH p.company
        JOIN FETCH p.provider
        WHERE pp.status = 'UNPAID' AND pp.dueDate = :dueDate AND pp.deleted = false
    """)
    List<PremiumPayment> findUnpaidByDueDate(@Param("dueDate") LocalDate dueDate);

    @Query("""
        SELECT pp FROM PremiumPayment pp
        JOIN FETCH pp.policy p
        JOIN FETCH p.company
        JOIN FETCH p.provider
        WHERE pp.id = :id AND pp.deleted = false
    """)
    Optional<PremiumPayment> findActiveById(@Param("id") String id);

    // ─── Aggregates ──────────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(pp.amount), 0) FROM PremiumPayment pp WHERE pp.status = 'PAID' AND pp.deleted = false")
    Double sumPaidAmount();

    @Query("SELECT COALESCE(SUM(pp.amount), 0) FROM PremiumPayment pp WHERE pp.status = 'UNPAID' AND pp.deleted = false")
    Double sumUnpaidAmount();

    @Query("SELECT COALESCE(SUM(pp.amount), 0) FROM PremiumPayment pp WHERE pp.status = 'OVERDUE' AND pp.deleted = false")
    Double sumOverdueAmount();

    @Query("SELECT COALESCE(SUM(pp.amount), 0) FROM PremiumPayment pp WHERE pp.deleted = false")
    Double sumTotalAmount();

    @Query("SELECT pp.policy.provider.name, SUM(pp.amount) FROM PremiumPayment pp WHERE pp.deleted = false GROUP BY pp.policy.provider.name")
    List<Object[]> sumByProvider();

    @Query("""
        SELECT pp.policy.company.name, SUM(pp.amount),
               SUM(CASE WHEN pp.status = 'PAID'   THEN pp.amount ELSE 0 END),
               SUM(CASE WHEN pp.status = 'UNPAID' THEN pp.amount ELSE 0 END)
        FROM PremiumPayment pp
        WHERE pp.deleted = false
        GROUP BY pp.policy.company.name
    """)
    List<Object[]> sumByCompany();

    // ─── Soft delete support ─────────────────────────────────────────────────

    @Query("SELECT pp FROM PremiumPayment pp WHERE pp.deleted = true")
    List<PremiumPayment> findAllDeleted();

    @Query("SELECT pp FROM PremiumPayment pp WHERE pp.id = :id AND pp.deleted = true")
    Optional<PremiumPayment> findDeletedById(@Param("id") String id);
}