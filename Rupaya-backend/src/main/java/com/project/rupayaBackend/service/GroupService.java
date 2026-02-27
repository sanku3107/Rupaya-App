package com.project.rupayaBackend.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.project.rupayaBackend.dto.*;
import com.project.rupayaBackend.repository.BillShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.project.rupayaBackend.entity.Group;
import com.project.rupayaBackend.entity.GroupMember;
import com.project.rupayaBackend.entity.User;
import com.project.rupayaBackend.entity.enums.GroupRole;
import com.project.rupayaBackend.exception.NotFoundException;
import com.project.rupayaBackend.repository.GroupMemberRepository;
import com.project.rupayaBackend.repository.GroupRepository;
import com.project.rupayaBackend.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class GroupService {
    @Autowired
    private BillShareRepository billShareRepository;

    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;

    Logger logger = LoggerFactory.getLogger(GroupService.class);

    public GroupResponse createGroup(GroupCreationRequest request, UUID creatorId) {
        if (request.getInitial_members() == null || request.getInitial_members().isEmpty()) {
            throw new IllegalArgumentException("Initial members cannot be empty");
        }

        User creator = userRepository.findById(creatorId).orElseThrow(() -> new NotFoundException("User not found"));
        //create group
        Group group = Group.builder().name(request.getName()).description(request.getDescription()).build();
        group.setCreatedBy(creatorId);
        groupRepository.save(group);

        //add creator as admin
        GroupMember creatorMember = GroupMember.builder().groupId(group.getId()).userId(creatorId).role(GroupRole.ADMIN).build();
        creatorMember.setCreatedBy(creatorId);
        groupMemberRepository.save(creatorMember);

        // Add initial members (skip self, skip invalid emails)
        // Spec: if none added (besides creator) -> rollback + ValidationError
        Set<String> emails = request.getInitial_members().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Remove creator email if present
        emails.remove(creator.getEmail().toLowerCase());

        int addedCount = 0;
        for (String email : emails) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) continue; // it "find user, add if exists" (you can change to error if you want)
            User u = userOpt.get();

            // prevent duplicates by constraint (user_id, group_id)
            // you can also check exists before save
            if (groupMemberRepository.existsActiveByGroupIdAndUserId(group.getId(), u.getId())) {
                continue;
            }

            GroupMember member = GroupMember.builder()
                    .groupId(group.getId())
                    .userId(u.getId())
                    .role(GroupRole.MEMBER)
                    .build();
            member.setCreatedBy(creatorId);
            groupMemberRepository.save(member);
            addedCount++;
        }

        if (addedCount == 0) {
            // No other members could be added -> rollback
            // If you have a custom ValidationException, throw that.
            throw new IllegalArgumentException("At least one valid member (other than yourself) must be added");
        }

        long memberCount = groupMemberRepository.countActiveMembers(group.getId());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .memberCount(memberCount)
                .build();
    }

    public PaginatedResponse<GroupResponse> getAllGroupsOfUser(
            UUID userId,
            int skip,
            int limit,
            String search
    ) {

        int page = skip / limit; // convert offset to page number

        Pageable pageable = PageRequest.of(page, limit);

        Page<Group> groupPage = groupRepository.findAllGroupsOfUser(userId, pageable, search);
        List<GroupResponse> items = groupPage.getContent().stream()
                .map(g -> GroupResponse.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .description(g.getDescription())
                        .createdBy(g.getCreatedBy())
                        .createdAt(g.getCreatedAt())
                        .memberCount(groupMemberRepository.countByGroupId(g.getId()))
                        .totalOwed(billShareRepository.sumTotalOwedInGroup(userId, g.getId()))
                        .totalOwe(billShareRepository.sumTotalOweInGroup(userId, g.getId()))
                        .build())
                .toList();

        return PaginatedResponse.<GroupResponse>builder()
                .items(items)
                .total(groupPage.getTotalElements())
                .skip(skip)
                .limit(limit)
                .hasMore(groupPage.hasNext())
                .build();
    }

    public GroupDetailResponse getGroupDetail(UUID groupId, UUID currentUserId) {

        // 1) Ensure group exists
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // 2) Ensure current user is member
        boolean isMember = groupMemberRepository.existsActiveByGroupIdAndUserId(groupId, currentUserId);
        if (!isMember) {
            // throw ForbiddenException -> 403
            throw new SecurityException("Not a member of this group");
        }

        // 3) Fetch members
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);

        // 4) Fetch user details for those members
        List<UUID> memberUserIds = members.stream()
                .map(GroupMember::getUserId)
                .distinct()
                .toList();

        Map<UUID, User> userMap = userRepository.findByIdIn(memberUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 5) Build members response
        List<GroupMembersResponse> memberResponses = members.stream()
                .map(m -> {
                    User u = userMap.get(m.getUserId());
                    UserResponse userDto = (u == null) ? null : UserResponse.builder()
                            .id(u.getId())
                            .name(u.getName())
                            .email(u.getEmail())
                            .role(u.getRole())
                            .build();

                    return GroupMembersResponse.builder()
                            .id(m.getId())
                            .user(userDto)
                            .role(m.getRole().name())
                            .createdAt(m.getCreatedAt())
                            .build();
                })
                .toList();

        return GroupDetailResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .memberCount(memberResponses.size())
                .totalOwed(billShareRepository.sumTotalOwedInGroup(currentUserId, group.getId()))
                .totalOwe(billShareRepository.sumTotalOweInGroup(currentUserId, group.getId()))
                .members(memberResponses)
                .build();
    }

    public GroupMembersResponse addMemberToGroup(UUID groupId, UUID loggedInUserId, AddMemberRequest request) {

        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        boolean isAdmin = groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, loggedInUserId, GroupRole.ADMIN);
        if (!isAdmin) throw new SecurityException("Not Admin, can't perform action");

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        String email = request.getEmail().trim().toLowerCase();
        User userToAdd = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email " + email));

        // Active membership exists -> already member
        if (groupMemberRepository.existsActiveByGroupIdAndUserId(groupId, userToAdd.getId())) {
            throw new IllegalArgumentException("member of this group already");
        }

        // check if a soft-deleted membership exists and reactivate it
        GroupMember member = groupMemberRepository
                .findAnyByGroupIdAndUserIdIncludingDeleted(groupId, userToAdd.getId())
                .orElse(null);

        if (member != null) {
            // Reactivate existing row (NO INSERT)
            member.setDeletedAt(null);
            member.setDeletedBy(null);
            member.setRole(GroupRole.MEMBER);
            member.setUpdatedBy(loggedInUserId);
            member = groupMemberRepository.save(member);
        } else {
            // No row exists at all -> create new
            member = GroupMember.builder()
                    .userId(userToAdd.getId())
                    .groupId(groupId)
                    .role(GroupRole.MEMBER)
                    .build();
            member.setCreatedBy(loggedInUserId);
            member.setUpdatedBy(loggedInUserId);
            member = groupMemberRepository.save(member);
        }

        UserResponse response = UserResponse.builder()
                .id(userToAdd.getId())
                .name(userToAdd.getName())
                .email(userToAdd.getEmail())
                .role(userToAdd.getRole())
                .build();

        return GroupMembersResponse.builder()
                .id(member.getId())
                .user(response)
                .role(member.getRole().name())
                .createdAt(member.getCreatedAt())
                .build();
    }


    public GroupMembersResponse makeMemberToAdmin(UUID groupId, UUID memberId, AddMemberRequest request, UUID loggedInUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        boolean isAdmin = groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, loggedInUserId, GroupRole.ADMIN);
        if (!isAdmin) {
            throw new SecurityException("Not Admin, can't perform action");
        }

        GroupMember gm = groupMemberRepository.findByIdAndGroupId(memberId, groupId).orElseThrow(() -> new NotFoundException("Member not found with id" + memberId));

        if (gm.getRole().equals(GroupRole.MEMBER)) {
            gm.setRole(GroupRole.ADMIN);
        } else {
            gm.setRole(GroupRole.MEMBER);
        }
        gm.setCreatedBy(loggedInUserId);
        GroupMember saved = groupMemberRepository.save(gm);

        UUID userId = saved.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id " + userId));
        UserResponse userResponse = (userId == null) ? null : UserResponse.builder().id(userId).name(user.getName()).email(user.getEmail()).role(user.getRole()).build();
        return GroupMembersResponse.builder().id(saved.getId()).user(userResponse).role(saved.getRole().name()).createdAt(saved.getCreatedAt()).build();
    }

    public GroupResponse updateGroupInfo(UpdateGroup updateGroup, UUID loggedUserId, UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!groupMemberRepository.existsActiveByGroupIdAndUserId(groupId, loggedUserId)) {
            throw new NotFoundException("User not found with id " + loggedUserId);
        }
        if (updateGroup.getName() != null && !updateGroup.getName().trim().isEmpty()) {
            group.setName(updateGroup.getName().trim());
            group.setUpdatedBy(loggedUserId);
        }
        groupRepository.save(group);

        GroupResponse response = GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .memberCount(groupMemberRepository.countActiveMembers(groupId))
                .totalOwe(billShareRepository.sumTotalOweInGroup(loggedUserId,groupId))
                .totalOwed(billShareRepository.sumTotalOwedInGroup(loggedUserId,groupId))
                .build();
        return response;
    }

    public void deleteGroup(UUID groupId, UUID loggedUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        boolean isAdmin = groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, loggedUserId, GroupRole.ADMIN);
        if (!isAdmin) {
            throw new SecurityException("Not Admin, can't perform action");
        }
        group.deletedBy(loggedUserId);
        groupRepository.save(group);
    }

    public void removeMemberFromGroup(UUID groupId, UUID memberId, UUID removedByUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        GroupMember member = groupMemberRepository.findByIdAndGroupId(memberId, groupId)
                .orElseThrow(() -> new NotFoundException("Group member not found"));

        boolean removingSelf = member.getUserId().equals(removedByUserId);

        if (removingSelf) {
            // if self is ADMIN and only admin, but group has other members -> block
            if (member.getRole() == GroupRole.ADMIN) {
                long adminCount = groupMemberRepository.countActiveByGroupIdAndRole(groupId, GroupRole.ADMIN);
                long otherMembers = groupMemberRepository.countOtherActiveMembers(groupId, removedByUserId);

                if (adminCount == 1 && otherMembers > 0) {
                    throw new IllegalArgumentException(
                            "To remove yourself please assign any other group member as admin. After that you can remove yourself."
                    );
                }
            }
        } else {
            // removing someone else => must be admin
            boolean isAdmin = groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, removedByUserId, GroupRole.ADMIN);
            if (!isAdmin) {
                throw new SecurityException("Not Admin, can't perform action");
            }
        }

        // Soft delete
        member.deletedBy(removedByUserId);
        member.setUpdatedBy(removedByUserId);
        groupMemberRepository.save(member);

        // If no active members left => auto soft delete group
        long remaining = groupMemberRepository.countActiveMembers(groupId);
        if (remaining == 0) {
            group.deletedBy(removedByUserId);
            group.setUpdatedBy(removedByUserId);
            groupRepository.save(group);
        }
    }
}


