package com.project.rupayaBackend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.rupayaBackend.dto.LoginResponse;
import com.project.rupayaBackend.dto.RegisterRequest;
import com.project.rupayaBackend.dto.UserResponse;
import com.project.rupayaBackend.security.CustomUserDetails;
import com.project.rupayaBackend.service.AuthService;
import com.project.rupayaBackend.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getUserInfo(userDetails));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> SearchUsers(@RequestParam("q") String q, @AuthenticationPrincipal CustomUserDetails principal) {
        UUID currentUserId = principal.getId();
        List<UserResponse> responses = userService.searchUsers(q, currentUserId);
        return ResponseEntity.ok(responses);
    }
}
