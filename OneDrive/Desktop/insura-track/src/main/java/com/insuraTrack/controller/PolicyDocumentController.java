package com.insuraTrack.controller;

import com.insuraTrack.model.PolicyDocument;
import com.insuraTrack.service.PolicyDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PolicyDocumentController {

    private final PolicyDocumentService policyDocumentService;

    @PostMapping("/policies/{policyId}/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyDocument> uploadForPolicy(
            @PathVariable String policyId,
            @RequestParam String docType,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(policyDocumentService.upload(policyId, file, docType));
    }

    @GetMapping("/policies/{policyId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<PolicyDocument>> getForPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(policyDocumentService.getByPolicy(policyId));
    }

    @GetMapping("/policies/{policyId}/documents/{docId}/download")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Resource> downloadForPolicy(
            @PathVariable String policyId,
            @PathVariable String docId) {
        return buildDownloadResponse(docId);
    }

    @DeleteMapping("/policies/{policyId}/documents/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteForPolicy(
            @PathVariable String policyId,
            @PathVariable String docId,
            @RequestParam String deletedBy) {
        policyDocumentService.softDelete(docId, deletedBy);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Resource> buildDownloadResponse(String docId) {
        PolicyDocument doc = policyDocumentService.getById(docId);
        try {
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path filePath    = projectRoot.resolve(doc.getFilePath());

            log.info("Resolved file path: {}", filePath.toAbsolutePath());

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found or not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            String contentType = doc.getFileType();
            String contentDisposition;

            if (contentType != null && (contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
                contentDisposition = "inline; filename=\"" + doc.getFileName() + "\"";
            } else {
                contentDisposition = "attachment; filename=\"" + doc.getFileName() + "\"";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            doc.getFileType() != null ? doc.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    // ✅ Allow JS blob-fetch from browser (CORS preflight)
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Error resolving file path for document: {}", docId, e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error while downloading document: {}", docId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}