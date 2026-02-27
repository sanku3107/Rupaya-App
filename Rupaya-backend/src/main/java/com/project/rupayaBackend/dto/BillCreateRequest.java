package com.project.rupayaBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.rupayaBackend.entity.enums.SplitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillCreateRequest {

    @NotNull
    @JsonProperty("group_id")
    private UUID groupId;

    @NotBlank
    private String description;

    @JsonProperty("paid_by")
    private UUID paidBy;

    @NotNull
    private List<BillShareRequest> shares;

    @Builder.Default
    @NotNull
    @JsonProperty("split_type")
    private SplitType splitType = SplitType.EQUAL;

    @NotNull
    @JsonProperty("total_amount")
    private Double totalAmount;
}
