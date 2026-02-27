package com.project.rupayaBackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreationRequest {

    @NotNull
    private String name;

    private String description;

    @NotNull
    @Builder.Default
    private List<String> initial_members = new ArrayList<>();

}
