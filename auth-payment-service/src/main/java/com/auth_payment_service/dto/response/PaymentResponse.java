package com.auth_payment_service.dto.response;

import java.math.BigDecimal;

public record PaymentResponse(
        String paymentId,
        String stripePaymentIntentId,
        String clientSecret,
        BigDecimal amount,
        String currency,
        String status
) {}
