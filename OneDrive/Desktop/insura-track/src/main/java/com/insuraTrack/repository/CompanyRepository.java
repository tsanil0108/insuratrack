package com.insuraTrack.repository;

import com.insuraTrack.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {

    List<Company> findAllByDeletedFalseAndActiveTrue();

    Optional<Company> findByIdAndDeletedFalse(String id);

    // ✅ FIX: Check duplicate name only among non-deleted companies
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);


    // ✅ For recycle bin — fetch soft-deleted companies
    @Query("SELECT c FROM Company c WHERE c.deleted = true")
    List<Company> findAllByDeletedTrue();
}