package com.project.rupayaBackend.service;

import com.project.rupayaBackend.dto.UserResponse;
import com.project.rupayaBackend.entity.User;
import com.project.rupayaBackend.exception.NotFoundException;
import com.project.rupayaBackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public UserResponse getUserInfo(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
        return response;
    }

    public List<UserResponse> searchUsers(String q, UUID currentUserId) {
        if (q == null || q.trim().isEmpty()) {
            return List.of();
        }
        List<User> users = userRepository.searchByNameOrEmailExcludeSelf(q.trim(), currentUserId, PageRequest.of(0, 10));
        List<UserResponse> userResponses = users.stream().map(user -> UserResponse.builder().id(user.getId()).name(user.getName()).email(user.getEmail()).role(user.getRole()).build()).toList();
        return userResponses;
    }
}
