package com.project.rupayaBackend.repository;

import com.project.rupayaBackend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.id <> :currentUserId
              AND (
                   LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    List<User> searchByNameOrEmailExcludeSelf(
            @Param("q") String q,
            @Param("currentUserId") UUID currentUserId,
            Pageable pageable
    );

    boolean existsByEmail(String email);

    List<User> findByIdIn(List<UUID> ids);

    List<User> findAllByIdIn(List<UUID> ids);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        WHERE u.id <> :userId
          AND u.id IN (
              SELECT gm2.userId
              FROM GroupMember gm2
              WHERE gm2.groupId IN (
                  SELECT gm1.groupId
                  FROM GroupMember gm1
                  WHERE gm1.userId = :userId
              )
          )
        ORDER BY u.createdAt DESC
    """)
    List<User> findFriendsForUser(@Param("userId") UUID userId, Pageable pageable);

}
