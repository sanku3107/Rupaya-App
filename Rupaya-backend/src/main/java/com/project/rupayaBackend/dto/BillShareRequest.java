package com.project.rupayaBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillShareRequest {

    @NotNull
    @JsonProperty("user_id")
    private UUID userId;

    private Double amount;
}
