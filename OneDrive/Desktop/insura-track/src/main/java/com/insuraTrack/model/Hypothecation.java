package com.insuraTrack.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hypothecations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hypothecation extends BaseEntity {

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private String mobileNumber;

    @Column(nullable = false)
    private String email;

    private boolean active = true;
}