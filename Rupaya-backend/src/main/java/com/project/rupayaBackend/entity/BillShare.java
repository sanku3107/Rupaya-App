package com.project.rupayaBackend.entity;

import com.project.rupayaBackend.entity.base.AuditableSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "bill_shares",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_bill_share_bill_user", columnNames = {"bill_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_bill_share_bill_id", columnList = "bill_id"),
                @Index(name = "idx_bill_share_user_id", columnList = "user_id"),
                @Index(name = "idx_bill_share_created_by", columnList = "created_by"),
                @Index(name = "idx_bill_share_updated_by", columnList = "updated_by"),
                @Index(name = "idx_bill_share_deleted_by", columnList = "deleted_by")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BillShare extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bill_id", nullable = false)
    private UUID billId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    @Builder.Default
    private Boolean paid = false;
}

