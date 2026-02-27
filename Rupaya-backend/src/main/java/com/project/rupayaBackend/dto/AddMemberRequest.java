package com.project.rupayaBackend.dto;

import com.project.rupayaBackend.entity.enums.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AddMemberRequest {
    private String email;
    @Builder.Default
    private String role = GroupRole.MEMBER.toString();
}
