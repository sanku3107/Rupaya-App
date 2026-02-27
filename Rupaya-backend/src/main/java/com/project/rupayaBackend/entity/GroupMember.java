package com.project.rupayaBackend.entity;

import com.project.rupayaBackend.entity.base.AuditableSoftDeleteEntity;
import com.project.rupayaBackend.entity.enums.GroupRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "group_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_group_member_user_group", columnNames = {"user_id", "group_id"})
        },
        indexes = {
                @Index(name = "idx_group_member_user_id", columnList = "user_id"),
                @Index(name = "idx_group_member_group_id", columnList = "group_id"),
                @Index(name = "idx_group_member_created_by", columnList = "created_by"),
                @Index(name = "idx_group_member_updated_by", columnList = "updated_by"),
                @Index(name = "idx_group_member_deleted_by", columnList = "deleted_by")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class GroupMember extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // You can map these as @ManyToOne if you want. UUID FK is simplest and fast.
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GroupRole role = GroupRole.MEMBER;
}

