package com.auth_payment_service.service;

import com.auth_payment_service.dto.request.LoginRequest;
import com.auth_payment_service.dto.request.RegisterRequest;
import com.auth_payment_service.dto.response.AuthResponse;
import com.auth_payment_service.dto.response.UserResponse;
import com.auth_payment_service.dto.enums.Role;
import com.auth_payment_service.entity.User;
import com.auth_payment_service.exception.DuplicateResourceException;
import com.auth_payment_service.exception.BadRequestException;
import com.auth_payment_service.messaging.UserEventPublisher;
import com.auth_payment_service.messaging.event.UserRegisteredEvent;
import com.auth_payment_service.repository.UserRepository;
import com.auth_payment_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserEventPublisher userEventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);

        userEventPublisher.publishUserRegistered(
                new UserRegisteredEvent(user.getId(), user.getEmail(), user.getFirstName())
        );

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new BadRequestException("This account has been disabled");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name());
        UserResponse userResponse = new UserResponse(
                user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(),
                user.getRole().name()
        );
        return new AuthResponse(token, refreshToken ,"Bearer", jwtTokenProvider.getExpiration(), userResponse);
    }
}
