package com.project.rupayaBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillResponse {

    private UUID id;

    @JsonProperty("group_id")
    private UUID groupId;

    @JsonProperty("paid_by")
    private UUID paidBy;

    // nested
    private UserResponse payer;
    private GroupMinimal group;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("split_type")
    private String splitType;

    private String description;

    @JsonProperty("total_amount")
    private Double totalAmount;

    private List<BillShareResponse> shares;
}

