package com.project.rupayaBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillShareResponse {
    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;
    private Double amount;
    private boolean paid;
    private UserResponse user;
}

