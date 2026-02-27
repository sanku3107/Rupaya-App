package com.project.rupayaBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDetailResponse {

    private UUID id;
    private String name;
    private String description;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("member_count")
    private long memberCount;

    @JsonProperty("total_owed")
    private double totalOwed;

    @JsonProperty("total_owe")
    private double totalOwe;
    private List<GroupMembersResponse> members;

}
