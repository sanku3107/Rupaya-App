package com.project.rupayaBackend.repository;

import com.project.rupayaBackend.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    @Query("""
                SELECT g
                FROM Group g
                WHERE g.id IN (
                    SELECT gm.groupId
                    FROM GroupMember gm
                    WHERE gm.userId = :userId
                ) AND (:search IS NULL OR :search = '' OR LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Group> findAllGroupsOfUser(@Param("userId") UUID userId, Pageable pageable, @Param("search") String search);

    List<Group> findAllByIdIn(List<UUID> ids);
}