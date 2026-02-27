package com.project.rupayaBackend.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMinimal {
    private UUID id;
    private String name;
}

