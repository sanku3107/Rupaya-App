package com.project.rupayaBackend.entity;

import com.project.rupayaBackend.entity.base.AuditableSoftDeleteEntity;
import com.project.rupayaBackend.entity.enums.SplitType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "bills",
        indexes = {
                @Index(name = "idx_bill_group_id", columnList = "group_id"),
                @Index(name = "idx_bill_paid_by", columnList = "paid_by"),
                @Index(name = "idx_bill_created_by", columnList = "created_by"),
                @Index(name = "idx_bill_updated_by", columnList = "updated_by"),
                @Index(name = "idx_bill_deleted_by", columnList = "deleted_by")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Bill extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "paid_by", nullable = false)
    private UUID paidBy;

    @Column(nullable = false)
    private String description;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    @Builder.Default
    private SplitType splitType = SplitType.EQUAL;
}

