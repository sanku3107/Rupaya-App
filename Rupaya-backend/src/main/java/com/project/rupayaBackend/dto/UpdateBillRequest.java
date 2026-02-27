package com.project.rupayaBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.rupayaBackend.entity.enums.SplitType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateBillRequest {
    private String description;

    @JsonProperty("paid_by")
    private UUID paidBy;

    private List<BillShareRequest> shares;

    private SplitType splitType;

    @JsonProperty("total_amount")
    private Double totalAmount;

}
