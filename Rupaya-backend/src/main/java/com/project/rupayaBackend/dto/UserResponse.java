package com.project.rupayaBackend.dto;

import com.project.rupayaBackend.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;  // serialized as string in JSON
    private String name;
    private String email;
    private Role role;
}
