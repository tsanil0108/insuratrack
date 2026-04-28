package com.insuraTrack.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceProvider {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(nullable = false)
    private String name;

    private String contactInfo;

    @Column(nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean deleted = false;

    private String deletedBy;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedBy = deletedBy;
        this.deletedAt = LocalDateTime.now();
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ ADD THIS RESTORE METHOD
    public void restore() {
        this.deleted = false;
        this.deletedBy = null;
        this.deletedAt = null;
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }
}