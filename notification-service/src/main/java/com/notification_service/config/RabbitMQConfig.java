package com.notification_service.config;

import com.notification_service.messaging.event.StripePaymentEvent;
import com.notification_service.messaging.event.UserRegisteredEvent;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String STRIPE_EXCHANGE      = "stripe.exchange";
    public static final String USER_EXCHANGE        = "user.exchange";
    public static final String NOTIFICATION_QUEUE   = "notification.queue";
    public static final String DEAD_LETTER_EXCHANGE = "stripe.dlx";
    public static final String NOTIFICATION_DLQ     = "notification.dlq";
    public static final String STRIPE_ROUTING_KEY   = "stripe.#";
    public static final String USER_ROUTING_KEY     = "user.#";

    @Bean
    public TopicExchange stripeExchange() {
        return ExchangeBuilder.topicExchange(STRIPE_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange userExchange() {
        return ExchangeBuilder.topicExchange(USER_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding stripeBinding(Queue notificationQueue, TopicExchange stripeExchange) {
        return BindingBuilder.bind(notificationQueue).to(stripeExchange).with(STRIPE_ROUTING_KEY);
    }

    @Bean
    public Binding userBinding(Queue notificationQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(notificationQueue).to(userExchange).with(USER_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(Queue notificationDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue).to(deadLetterExchange).with(NOTIFICATION_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setIdClassMapping(Map.of(
                "com.auth_payment_service.messaging.event.StripePaymentEvent", StripePaymentEvent.class,
                "com.auth_payment_service.messaging.event.UserRegisteredEvent", UserRegisteredEvent.class
        ));
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }
}
