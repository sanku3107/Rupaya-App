package com.project.rupayaBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupMembersResponse {
    private UUID id; // group_members.id

    private UserResponse user;

    private String role;

    @JsonProperty("created_at")
    private Instant createdAt;

}
