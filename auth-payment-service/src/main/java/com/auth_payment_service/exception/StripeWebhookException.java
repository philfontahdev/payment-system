package com.auth_payment_service.exception;

public class StripeWebhookException extends RuntimeException {

    public StripeWebhookException(String message) {
        super(message);
    }

    public StripeWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
