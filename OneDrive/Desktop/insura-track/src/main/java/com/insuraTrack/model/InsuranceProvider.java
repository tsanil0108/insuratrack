package com.insuraTrack.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insurance_providers")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceProvider extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;  // ICICI Lombard, SBI General, HDFC Ergo

    private String contactInfo;

    private boolean active = true;
}
