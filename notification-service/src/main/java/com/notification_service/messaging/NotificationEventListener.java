package com.notification_service.messaging;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.notification_service.config.RabbitMQConfig;
import com.notification_service.exception.EmailDeliveryException;
import com.notification_service.exception.MessageDeserializationException;
import com.notification_service.messaging.event.StripePaymentEvent;
import com.notification_service.messaging.event.UserRegisteredEvent;
import com.notification_service.service.EmailService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleEvent(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) throws IOException {

        try {
            log.info("Received event [routingKey={}]", routingKey);

            switch (routingKey) {
                case "user.registered"          -> handleUserRegistered(message);
                case "stripe.payment.created"   -> handlePaymentCreated(message);
                case "stripe.payment.completed" -> handlePaymentCompleted(message);
                case "stripe.payment.failed"    -> handlePaymentFailed(message);
                case "stripe.payment.canceled"  -> handlePaymentCanceled(message);
                default -> log.warn("No handler for routing key: {}", routingKey);
            }

            channel.basicAck(deliveryTag, false);

        } catch (EmailDeliveryException e) {
            log.error("Email delivery failed [routingKey={}], requeueing", routingKey, e);
            channel.basicNack(deliveryTag, false, true);

        } catch (MessageDeserializationException e) {
            log.error("Unreadable message [routingKey={}], sending to DLQ", routingKey, e);
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            log.error("Unexpected error processing event [routingKey={}], sending to DLQ", routingKey, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void handleUserRegistered(Message message) {
        UserRegisteredEvent event = deserialize(message, UserRegisteredEvent.class, "user.registered");
        emailService.sendWelcomeEmail(event.email(), event.firstName());
    }

    private void handlePaymentCreated(Message message) {
        StripePaymentEvent event = deserialize(message, StripePaymentEvent.class, "stripe.payment.created");
        emailService.sendPaymentCreatedEmail(
                event.email(), event.paymentId(),
                event.amount().toPlainString(), event.currency()
        );
    }

    private void handlePaymentCompleted(Message message) {
        StripePaymentEvent event = deserialize(message, StripePaymentEvent.class, "stripe.payment.completed");
        emailService.sendPaymentCompletedEmail(
                event.email(), event.paymentId(),
                event.amount().toPlainString(), event.currency()
        );
    }

    private void handlePaymentFailed(Message message) {
        StripePaymentEvent event = deserialize(message, StripePaymentEvent.class, "stripe.payment.failed");
        emailService.sendPaymentFailedEmail(event.email(), event.paymentId());
    }

    private void handlePaymentCanceled(Message message) {
        StripePaymentEvent event = deserialize(message, StripePaymentEvent.class, "stripe.payment.canceled");
        emailService.sendPaymentCanceledEmail(event.email(), event.paymentId());
    }

    private <T> T deserialize(Message message, Class<T> type, String routingKey) {
        try {
            return objectMapper.readValue(message.getBody(), type);
        } catch (Exception e) {
            throw new MessageDeserializationException(routingKey, e);
        }
    }
}
