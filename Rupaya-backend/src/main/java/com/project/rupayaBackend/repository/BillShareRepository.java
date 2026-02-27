package com.project.rupayaBackend.repository;

import com.project.rupayaBackend.entity.BillShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BillShareRepository extends JpaRepository<BillShare, UUID> {
    List<BillShare> findByBillId(UUID billId);

    List<BillShare> findByBillIdIn(List<UUID> billIds);

    @Query("""
    SELECT COALESCE(SUM(bs.amount), 0)
    FROM BillShare bs
    JOIN Bill b ON b.id = bs.billId
    WHERE b.groupId = :groupId
      AND b.paidBy = :userId
      AND bs.userId <> :userId
      AND bs.paid = false
""")
    double sumTotalOwedInGroup(@Param("userId") UUID userId, @Param("groupId") UUID groupId);

    @Query("""
        SELECT COALESCE(SUM(bs.amount), 0)
        FROM BillShare bs
        JOIN Bill b ON b.id = bs.billId
        WHERE b.paidBy = :userId
          AND bs.userId <> :userId
          AND bs.paid = false
    """)
    double sumTotalOwedAll(@Param("userId") UUID userId);

    @Query("""
    SELECT COALESCE(SUM(bs.amount), 0)
    FROM BillShare bs
    JOIN Bill b ON b.id = bs.billId
    WHERE b.groupId = :groupId
      AND bs.userId = :userId
      AND b.paidBy <> :userId
      AND bs.paid = false
""")
    double sumTotalOweInGroup(@Param("userId") UUID userId, @Param("groupId") UUID groupId);

    @Query("""
        SELECT COALESCE(SUM(bs.amount), 0)
        FROM BillShare bs
        JOIN Bill b ON b.id = bs.billId
        WHERE bs.userId = :userId
          AND b.paidBy <> :userId
          AND bs.paid = false
    """)
    double sumTotalOweAll(@Param("userId") UUID userId);
}