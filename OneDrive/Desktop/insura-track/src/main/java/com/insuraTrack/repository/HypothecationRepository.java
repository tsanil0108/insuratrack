package com.insuraTrack.repository;

import com.insuraTrack.model.Hypothecation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HypothecationRepository extends JpaRepository<Hypothecation, String> {

    List<Hypothecation> findAllByDeletedFalseAndActiveTrue();

    // ✅ ADD THIS METHOD - For finding deleted hypothecations
    @Query("SELECT h FROM Hypothecation h WHERE h.deleted = true")
    List<Hypothecation> findAllByDeletedTrue();
}