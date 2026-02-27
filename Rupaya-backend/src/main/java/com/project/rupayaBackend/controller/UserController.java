package com.project.rupayaBackend.controller;

import com.project.rupayaBackend.dto.LoginResponse;
import com.project.rupayaBackend.dto.RegisterRequest;
import com.project.rupayaBackend.dto.UserResponse;
import com.project.rupayaBackend.repository.UserRepository;
import com.project.rupayaBackend.security.CustomUserDetails;
import com.project.rupayaBackend.service.AuthService;
import com.project.rupayaBackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
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
