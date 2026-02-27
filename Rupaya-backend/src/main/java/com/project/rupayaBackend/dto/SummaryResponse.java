package com.project.rupayaBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SummaryResponse {

    @JsonProperty("total_owed")
    private Double totalOwed;

    @JsonProperty("total_owe")
    private Double totalOwe;

    @JsonProperty("group_count")
    private Long groupCount;

    private List<UserResponse> friends;
}
