package com.project.rupayaBackend.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.rupayaBackend.entity.Bill;

public interface BillRepository extends JpaRepository<Bill, UUID> {
    @Query("""
                SELECT DISTINCT b
                FROM Bill b
                WHERE b.paidBy = :userId
                   OR EXISTS (
                        SELECT 1 FROM BillShare s
                        WHERE s.billId = b.id AND s.userId = :userId
                   )
                ORDER BY b.createdAt DESC
            """)
    Page<Bill> findAllForUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
                SELECT b 
                FROM Bill b WHERE b.groupId = :groupId
                AND (:search IS NULL OR :search = '' OR LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%')))
                ORDER BY b.createdAt DESC
            """)
    Page<Bill> findAllByGroupId(@Param("groupId") UUID groupId, Pageable pageable, @Param("search") String search);
}