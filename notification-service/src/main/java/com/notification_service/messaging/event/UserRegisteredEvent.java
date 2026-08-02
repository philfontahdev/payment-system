package com.notification_service.messaging.event;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email,
        String firstName
) {}
