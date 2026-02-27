package com.project.rupayaBackend.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.rupayaBackend.entity.enums.SplitType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
