package com.project.rupayaBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    private List<T> items;
    private long total;
    private int skip;
    private int limit;

    @JsonProperty("has_more")
    private boolean hasMore;
}

