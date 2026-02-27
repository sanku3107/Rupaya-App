package com.project.rupayaBackend.repository;

import com.project.rupayaBackend.entity.GroupMember;
import com.project.rupayaBackend.entity.enums.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    @Query("""
                SELECT COUNT(gm) 
                FROM GroupMember gm
                WHERE gm.groupId = :groupId
            """)
    long countActiveMembers(@Param("groupId") UUID groupId);

    @Query("""
                SELECT (COUNT(gm) > 0)
                FROM GroupMember gm
                WHERE gm.groupId = :groupId
                  AND gm.userId = :userId
            """)
    boolean existsActiveByGroupIdAndUserId(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

    long countByGroupId(UUID groupId);

    List<GroupMember> findByGroupId(UUID groupId);

    boolean existsByGroupIdAndUserIdAndRole(
            UUID groupId,
            UUID userId,
            GroupRole role
    );

    Optional<GroupMember> findByIdAndGroupId(UUID id, UUID groupId);

    @Query("""
                SELECT COUNT(gm)
                FROM GroupMember gm
                WHERE gm.groupId = :groupId
                  AND gm.role = :role
            """)
    long countActiveByGroupIdAndRole(@Param("groupId") UUID groupId, @Param("role") GroupRole role);

    @Query("""
                SELECT COUNT(gm)
                FROM GroupMember gm
                WHERE gm.groupId = :groupId
                  AND gm.userId <> :userId
            """)
    long countOtherActiveMembers(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

    @Query(value = """
                SELECT *
                FROM group_members
                WHERE group_id = :groupId
                  AND user_id = :userId
                LIMIT 1
            """, nativeQuery = true)
    Optional<GroupMember> findAnyByGroupIdAndUserIdIncludingDeleted(@Param("groupId") UUID groupId,
                                                                    @Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(DISTINCT gm.groupId)
        FROM GroupMember gm
        WHERE gm.userId = :userId
    """)
    long countDistinctGroupsOfUser(@Param("userId") UUID userId);

    @Query("""
        SELECT DISTINCT gm.groupId
        FROM GroupMember gm
        WHERE gm.userId = :userId
    """)
    List<UUID> findDistinctGroupIdsOfUser(@Param("userId") UUID userId);
}
