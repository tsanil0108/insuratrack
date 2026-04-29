package com.insuraTrack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insurance_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceItem extends BaseEntity {

    // ✅ FIX: nullable = false removed → allows items with no insurance type
    //         Without this fix, items created without a type crash the /all endpoint
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_type_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private InsuranceType insuranceType;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}