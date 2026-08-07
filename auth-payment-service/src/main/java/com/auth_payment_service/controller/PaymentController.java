package com.auth_payment_service.controller;

import com.auth_payment_service.dto.request.PaymentRequest;
import com.auth_payment_service.dto.response.ApiResponse;
import com.auth_payment_service.dto.response.PaymentResponse;
import com.auth_payment_service.entity.User;
import com.auth_payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkout(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.createPayment(user.getUsername(), request);
        return ResponseEntity.ok(
                ApiResponse.<PaymentResponse>builder()
                        .message("Payment initiated successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<Void> webhook(
            @RequestBody byte[] payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("[WEBHOOK] Received — payload size={} bytes, sig-header present={}", payload.length, sigHeader != null);
        paymentService.handleWebhookEvent(new String(payload, StandardCharsets.UTF_8), sigHeader);
        return ResponseEntity.ok().build();
    }
}
