package com.auth_payment_service.controller;

import com.auth_payment_service.dto.request.LoginRequest;
import com.auth_payment_service.dto.request.RegisterRequest;
import com.auth_payment_service.dto.response.ApiResponse;
import com.auth_payment_service.dto.response.AuthResponse;
import com.auth_payment_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<AuthResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("User registered successfully")
                        .data(authService.register(request))
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .data(authService.login(request))
                        .build()
        );
    }
}
