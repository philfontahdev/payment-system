package com.auth_payment_service.messaging.event;

import java.math.BigDecimal;

public record StripePaymentEvent(
        String paymentId,
        String stripePaymentIntentId,
        String userId,
        String email,
        BigDecimal amount,
        String currency,
        String status
) {}
