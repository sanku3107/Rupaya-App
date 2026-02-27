package com.project.rupayaBackend.controller;

import com.project.rupayaBackend.dto.*;
import com.project.rupayaBackend.repository.GroupRepository;
import com.project.rupayaBackend.security.CustomUserDetails;
import com.project.rupayaBackend.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/v1/groups/")
@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupRepository groupRepository;
    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupCreationRequest body, @AuthenticationPrincipal CustomUserDetails principal) {
        UUID currentUserId = principal.getId();
        GroupResponse response = groupService.createGroup(body, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<GroupResponse>> getGroups(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(
                groupService.getAllGroupsOfUser(principal.getId(), skip, limit, search)
        );
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailResponse> getGroupDetails(@PathVariable UUID groupId, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(groupService.getGroupDetail(groupId, principal.getId()));
    }

    @PostMapping("{groupId}/members")
    public ResponseEntity<GroupMembersResponse> addMembers(@PathVariable UUID groupId, @AuthenticationPrincipal CustomUserDetails principal, @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(groupService.addMemberToGroup(groupId, principal.getId(), request));
    }

    @PatchMapping("{groupId}/members/{memberId}")
    public ResponseEntity<GroupMembersResponse> makeGroupMemberAdmin(@PathVariable UUID groupId, @PathVariable UUID memberId, @RequestBody AddMemberRequest request, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(groupService.makeMemberToAdmin(groupId, memberId, request, principal.getId()));
    }

    @PatchMapping("{groupId}")
    public ResponseEntity<GroupResponse> updateGroup(@RequestBody UpdateGroup updateGroup, @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.updateGroupInfo(updateGroup, principal.getId(), groupId));
    }

    @DeleteMapping("{groupId}")
    public ResponseEntity<MessageResponse> deleteGroup(@PathVariable UUID groupId, @AuthenticationPrincipal CustomUserDetails principal) {
        groupService.deleteGroup(groupId, principal.getId());
        return ResponseEntity.ok(new MessageResponse("Group deleted successfully"));
    }

    @DeleteMapping("{groupId}/members/{memberId}")
    public ResponseEntity<MessageResponse> removeMemberFromGroup(@PathVariable UUID groupId, @PathVariable UUID memberId, @AuthenticationPrincipal CustomUserDetails principal) {
        groupService.removeMemberFromGroup(groupId, memberId, principal.getId());
        return ResponseEntity.ok(new MessageResponse("Member removed successfully"));
    }
}
