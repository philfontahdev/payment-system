package com.auth_payment_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Stripe exchange — payment lifecycle events
    public static final String STRIPE_EXCHANGE       = "stripe.exchange";
    public static final String ROUTING_KEY_CREATED   = "stripe.payment.created";
    public static final String ROUTING_KEY_COMPLETED = "stripe.payment.completed";
    public static final String ROUTING_KEY_FAILED    = "stripe.payment.failed";
    public static final String ROUTING_KEY_CANCELED  = "stripe.payment.canceled";

    // User exchange — user lifecycle events
    public static final String USER_EXCHANGE                  = "user.exchange";
    public static final String ROUTING_KEY_USER_REGISTERED    = "user.registered";

    // Dead letter
    public static final String DEAD_LETTER_EXCHANGE = "stripe.dlx";
    public static final String DEAD_LETTER_QUEUE    = "stripe.dlq";

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
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_QUEUE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
