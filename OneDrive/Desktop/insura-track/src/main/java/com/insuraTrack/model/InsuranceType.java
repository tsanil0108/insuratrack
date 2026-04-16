package com.insuraTrack.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insurance_types")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceType extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;  // Factory, Workman, Vehicle, Health, Fire

    private String description;

    private boolean active = true;
}