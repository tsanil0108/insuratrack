package com.insuraTrack.service;

import com.insuraTrack.model.PolicyDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PolicyDocumentService {
    PolicyDocument upload(String policyId, MultipartFile file, String docType);
    PolicyDocument getById(String id);
    List<PolicyDocument> getByPolicy(String policyId);
    List<PolicyDocument> getByPolicyAndType(String policyId, String docType);
    void softDelete(String id, String deletedByEmail);
}