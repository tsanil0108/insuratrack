package com.insuraTrack.service;

import com.insuraTrack.model.Hypothecation;
import java.util.List;

public interface HypothecationService {
    Hypothecation create(Hypothecation hypothecation);
    Hypothecation getById(String id);
    List<Hypothecation> getAll();
    Hypothecation update(String id, Hypothecation updated);
    void softDelete(String id, String deletedByEmail);
}