package com.project.rupayaBackend.entity;

import com.project.rupayaBackend.entity.base.AuditableSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "groups",
        indexes = {
                @Index(name = "idx_group_name", columnList = "name"),
                @Index(name = "idx_group_created_by", columnList = "created_by"),
                @Index(name = "idx_group_updated_by", columnList = "updated_by"),
                @Index(name = "idx_group_deleted_by", columnList = "deleted_by")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Group extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String description;
}
