package com.insuraTrack.repository;

import com.insuraTrack.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, String> {

    List<Company> findByActiveTrueAndDeletedFalse();

    default List<Company> findByActiveTrue() {
        return findByActiveTrueAndDeletedFalse();
    }

    // ✅ ADD THIS METHOD
    @Query("SELECT c FROM Company c WHERE c.id = :id AND c.deleted = false")
    Optional<Company> findActiveById(@Param("id") String id);

    @Query("SELECT c FROM Company c WHERE c.deleted = true")
    List<Company> findAllDeleted();

    @Query("SELECT c FROM Company c WHERE c.id = :id AND c.deleted = true")
    Optional<Company> findDeletedById(@Param("id") String id);
}