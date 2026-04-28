package com.insuraTrack.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HypothecationDTO {

    private String id;

    private String bankName;

    private String employeeName;

    private String mobileNumber;

    private String email;

    private boolean active;
}