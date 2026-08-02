package com.notification_service.exception;

public class MessageDeserializationException extends RuntimeException {
    public MessageDeserializationException(String routingKey, Throwable cause) {
        super("Failed to deserialize message [routingKey=" + routingKey + "]", cause);
    }
}
