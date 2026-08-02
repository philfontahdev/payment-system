package com.notification_service.service;

import com.notification_service.exception.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendWelcomeEmail(String to, String firstName) {
        send(to, "Welcome to Payment System!",
                "Hi " + firstName + ",\n\n" +
                "Your account has been created successfully.\n\n" +
                "The Payment System Team");
    }

    public void sendPaymentCreatedEmail(String to, String paymentId, String amount, String currency) {
        send(to, "Payment Received",
                "Your payment of " + amount + " " + currency.toUpperCase() +
                " is being processed.\n\nPayment ID: " + paymentId);
    }

    public void sendPaymentCompletedEmail(String to, String paymentId, String amount, String currency) {
        send(to, "Payment Successful",
                "Your payment of " + amount + " " + currency.toUpperCase() +
                " was completed.\n\nPayment ID: " + paymentId);
    }

    public void sendPaymentFailedEmail(String to, String paymentId) {
        send(to, "Payment Failed",
                "Your payment (ID: " + paymentId + ") could not be processed. " +
                "Please try again or use a different payment method.");
    }

    public void sendPaymentCanceledEmail(String to, String paymentId) {
        send(to, "Payment Canceled",
                "Your payment (ID: " + paymentId + ") has been canceled. " +
                "If this was unexpected, please contact support.");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent [to={}, subject={}]", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email [to={}, subject={}]", to, subject, e);
            throw new EmailDeliveryException("Failed to send email to " + to, e);
        }
    }
}
