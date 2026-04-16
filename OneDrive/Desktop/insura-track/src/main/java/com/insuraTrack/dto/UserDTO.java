package com.insuraTrack.dto;

import com.insuraTrack.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private String id;
    private String name;
    private String email;
    private Role role;
    private boolean active;
}