package com.project.rupayaBackend.controller;

import com.project.rupayaBackend.dto.LoginRequest;
import com.project.rupayaBackend.dto.LoginResponse;
import com.project.rupayaBackend.dto.RefreshTokenRequest;
import com.project.rupayaBackend.dto.RegisterRequest;
import com.project.rupayaBackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping(value = "/login", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<LoginResponse> loginForm(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {
        LoginRequest request = LoginRequest.builder().username(username).password(password).build();
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }
}
