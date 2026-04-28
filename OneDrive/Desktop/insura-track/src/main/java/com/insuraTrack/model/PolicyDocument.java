package com.insuraTrack.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDocument extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false)
    private String fileName;

    private String storedFileName;  // ✅ Add this - for unique stored name

    @Column(nullable = false)
    private String filePath;

    private String fileType;

    private Long fileSize;

    /** POLICY_DOCUMENT or PAYMENT_SLIP */
    private String docType;

    // ✅ Add these fields for soft delete functionality
    private boolean deleted = false;

    private String deletedBy;

    private LocalDateTime deletedAt;

    private LocalDateTime uploadedAt;

    // ✅ Helper method for soft delete
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedBy = deletedBy;
        this.deletedAt = LocalDateTime.now();
    }
}