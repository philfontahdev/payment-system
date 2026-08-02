package com.auth_payment_service.messaging;

import com.auth_payment_service.config.RabbitMQConfig;
import com.auth_payment_service.messaging.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing user.registered event for userId={}", event.userId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, RabbitMQConfig.ROUTING_KEY_USER_REGISTERED, event);
    }
}
