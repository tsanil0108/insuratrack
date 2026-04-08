package com.insuraTrack.repository;

import com.insuraTrack.model.InsuranceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceTypeRepository extends JpaRepository<InsuranceType, String> {

    List<InsuranceType> findByActiveTrue();

    Optional<InsuranceType> findByNameIgnoreCase(String name);

    List<InsuranceType> findByNameContainingIgnoreCase(String name);
}