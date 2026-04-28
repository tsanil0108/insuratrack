package com.insuraTrack.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String shortName;
    private String address;
    private String city;
    private String district;
    private String state;
    private String pinCode;
    private String contactEmail;
    private String contactPhone;

    private boolean active = true;
}