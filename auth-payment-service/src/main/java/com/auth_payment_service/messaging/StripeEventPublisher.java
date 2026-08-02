package com.auth_payment_service.messaging;

import com.auth_payment_service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripeEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCreated(Object event) {
        publish(RabbitMQConfig.ROUTING_KEY_CREATED, event);
    }

    public void publishPaymentCompleted(Object event) {
        publish(RabbitMQConfig.ROUTING_KEY_COMPLETED, event);
    }

    public void publishPaymentFailed(Object event) {
        publish(RabbitMQConfig.ROUTING_KEY_FAILED, event);
    }

    public void publishPaymentCanceled(Object event) {
        publish(RabbitMQConfig.ROUTING_KEY_CANCELED, event);
    }

    private void publish(String routingKey, Object event) {
        log.info("Publishing Stripe event [routingKey={}]", routingKey);
        rabbitTemplate.convertAndSend(RabbitMQConfig.STRIPE_EXCHANGE, routingKey, event);
    }
}
