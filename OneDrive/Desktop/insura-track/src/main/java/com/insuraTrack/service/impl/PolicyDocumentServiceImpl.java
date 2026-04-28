package com.insuraTrack.service.impl;

import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.Policy;
import com.insuraTrack.model.PolicyDocument;
import com.insuraTrack.repository.PolicyDocumentRepository;
import com.insuraTrack.repository.PolicyRepository;
import com.insuraTrack.service.PolicyDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PolicyDocumentServiceImpl implements PolicyDocumentService {

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyRepository policyRepository;

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    @Override
    public PolicyDocument upload(String policyId, MultipartFile file, String docType) {
        log.info("========================================");
        log.info("Uploading document for policy: {}", policyId);
        log.info("Document type: {}", docType);
        log.info("File name: {}", file.getOriginalFilename());
        log.info("========================================");

        try {
            // ✅ Use findById only — no lazy proxy access
            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

            if (file.isEmpty()) {
                throw new RuntimeException("Cannot upload empty file");
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("File size exceeds 10MB limit");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String storedFileName = UUID.randomUUID() + extension;

            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path uploadPath  = projectRoot.resolve(uploadDir).resolve(policyId);
            log.info("Target directory: {}", uploadPath.toAbsolutePath());

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("✅ Created directory: {}", uploadPath.toAbsolutePath());
            }

            Path filePath = uploadPath.resolve(storedFileName);
            file.transferTo(filePath.toFile());
            log.info("✅ File saved to: {}", filePath.toAbsolutePath());

            // Store relative path from project root
            String relativePath = uploadDir + "/" + policyId + "/" + storedFileName;

            PolicyDocument doc = PolicyDocument.builder()
                    .policy(policy)
                    .fileName(originalFilename)
                    .storedFileName(storedFileName)
                    .filePath(relativePath)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .docType(docType)
                    .deleted(false)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            PolicyDocument saved = policyDocumentRepository.save(doc);
            log.info("✅ Document saved to database with ID: {}", saved.getId());
            log.info("========================================");

            return saved;

        } catch (IOException e) {
            log.error("❌ Failed to store file for policy: {}", policyId, e);
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    public PolicyDocument getById(String id) {
        return policyDocumentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("PolicyDocument not found: " + id));
    }

    @Override
    public List<PolicyDocument> getByPolicy(String policyId) {
        return policyDocumentRepository.findAllByPolicyIdAndDeletedFalse(policyId);
    }

    @Override
    public List<PolicyDocument> getByPolicyAndType(String policyId, String docType) {
        return policyDocumentRepository.findAllByPolicyIdAndDocTypeAndDeletedFalse(policyId, docType);
    }

    @Override
    public void softDelete(String id, String deletedByEmail) {
        PolicyDocument doc = getById(id);
        doc.softDelete(deletedByEmail);
        policyDocumentRepository.save(doc);
        log.info("Document soft deleted: {} by {}", id, deletedByEmail);
    }
}