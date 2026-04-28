package com.insuraTrack.repository;

import com.insuraTrack.model.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, String> {

    List<PolicyDocument> findAllByPolicyIdAndDeletedFalse(String policyId);

    List<PolicyDocument> findAllByPolicyIdAndDocTypeAndDeletedFalse(String policyId, String docType);
}