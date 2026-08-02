package com.auth_payment_service.service;

import com.auth_payment_service.dto.enums.PaymentStatus;
import com.auth_payment_service.dto.request.PaymentRequest;
import com.auth_payment_service.dto.response.PaymentResponse;
import com.auth_payment_service.entity.Payment;
import com.auth_payment_service.entity.User;
import com.auth_payment_service.exception.ResourceNotFoundException;
import com.auth_payment_service.exception.StripePaymentException;
import com.auth_payment_service.exception.StripeWebhookException;
import com.auth_payment_service.messaging.StripeEventPublisher;
import com.auth_payment_service.messaging.event.StripePaymentEvent;
import com.auth_payment_service.repository.PaymentRepository;
import com.auth_payment_service.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final StripeEventPublisher stripeEventPublisher;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public PaymentResponse createPayment(String email, PaymentRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String referenceId = generateReference();
        BigDecimal amount = BigDecimal.valueOf(request.amount()).movePointLeft(2);

        PaymentIntent intent = createStripeIntent(request, referenceId);

        Payment payment = Payment.builder()
                .referenceId(referenceId)
                .user(user)
                .stripePaymentIntentId(intent.getId())
                .amount(amount)
                .currency(request.currency().toLowerCase())
                .description(request.description())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        final UUID paymentId = payment.getId();
        final String currency = payment.getCurrency();
        publishSafely(() -> stripeEventPublisher.publishPaymentCreated(new StripePaymentEvent(
                paymentId.toString(),
                intent.getId(),
                user.getId().toString(),
                user.getEmail(),
                amount,
                currency,
                PaymentStatus.PENDING.name()
        )), paymentId);

        log.info("Payment created [referenceId={}, intentId={}]", referenceId, intent.getId());

        return new PaymentResponse(
                paymentId.toString(),
                intent.getId(),
                intent.getClientSecret(),
                amount,
                payment.getCurrency(),
                payment.getStatus().name()
        );
    }

    private static final String REFERENCE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private String generateReference() {
        RandomGenerator rng = RandomGenerator.getDefault();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(REFERENCE_CHARS.charAt(rng.nextInt(REFERENCE_CHARS.length())));
        }
        return sb.toString();
    }

    @Transactional
    public void handleWebhookEvent(String payload, String sigHeader) {
        log.info("[WEBHOOK] Attempting signature verification");
        Event event = verifyAndParse(payload, sigHeader);
        log.info("[WEBHOOK] Signature verified — eventId={}, type={}", event.getId(), event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded"      -> handleSucceeded(event);
            case "payment_intent.payment_failed" -> handleFailed(event);
            case "payment_intent.canceled"       -> handleCanceled(event);
            default -> log.warn("[WEBHOOK] Unhandled event type: {}", event.getType());
        }
    }

    private void handleSucceeded(Event event) {
        String intentId = extractIntentId(event);
        paymentRepository.findByStripePaymentIntentId(intentId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                log.debug("Duplicate webhook ignored [intentId={}]", intentId);
                return;
            }
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            publishSafely(() -> stripeEventPublisher.publishPaymentCompleted(
                    toEvent(payment, intentId, PaymentStatus.COMPLETED)), payment.getId());
            log.info("Payment completed [paymentId={}]", payment.getId());
        });
    }

    private void handleFailed(Event event) {
        String intentId = extractIntentId(event);
        paymentRepository.findByStripePaymentIntentId(intentId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.FAILED) {
                log.debug("Duplicate webhook ignored [intentId={}]", intentId);
                return;
            }
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            publishSafely(() -> stripeEventPublisher.publishPaymentFailed(
                    toEvent(payment, intentId, PaymentStatus.FAILED)), payment.getId());
            log.info("Payment failed [paymentId={}]", payment.getId());
        });
    }

    private void handleCanceled(Event event) {
        String intentId = extractIntentId(event);
        paymentRepository.findByStripePaymentIntentId(intentId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.CANCELED) {
                log.debug("Duplicate webhook ignored [intentId={}]", intentId);
                return;
            }
            payment.setStatus(PaymentStatus.CANCELED);
            paymentRepository.save(payment);
            publishSafely(() -> stripeEventPublisher.publishPaymentCanceled(
                    toEvent(payment, intentId, PaymentStatus.CANCELED)), payment.getId());
            log.info("Payment canceled [paymentId={}]", payment.getId());
        });
    }

    private StripePaymentEvent toEvent(Payment payment, String intentId, PaymentStatus status) {
        return new StripePaymentEvent(
                payment.getId().toString(),
                intentId,
                payment.getUser().getId().toString(),
                payment.getUser().getEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                status.name()
        );
    }

    private PaymentIntent createStripeIntent(PaymentRequest request, String referenceId) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.amount())
                    .setCurrency(request.currency().toLowerCase())
                    .setDescription(request.description())
                    .putMetadata("reference_id", referenceId)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(referenceId)
                    .build();

            return PaymentIntent.create(params, options);
        } catch (StripeException e) {
            log.error("Stripe API error", e);
            throw new StripePaymentException("Failed to initiate payment: " + e.getMessage(), e);
        }
    }

    private Event verifyAndParse(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("[WEBHOOK] Signature OK — eventId={}", event.getId());
            return event;
        } catch (SignatureVerificationException e) {
            log.error("[WEBHOOK] Signature FAILED — sigHeader={}, reason={}", sigHeader, e.getMessage());
            throw new StripeWebhookException("Invalid webhook signature", e);
        }
    }

    private String extractIntentId(Event event) {
        Optional<StripeObject> obj = event.getDataObjectDeserializer().getObject();
        if (obj.isPresent()) {
            return ((PaymentIntent) obj.get()).getId();
        }
        // SDK/API version mismatch — extract only the id field from raw JSON
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        if (rawJson == null || rawJson.isEmpty()) {
            throw new StripeWebhookException("Could not deserialize PaymentIntent from webhook");
        }
        log.warn("[WEBHOOK] API version mismatch — extracting intent ID from raw JSON");
        return JsonParser.parseString(rawJson).getAsJsonObject().get("id").getAsString();
    }

    // RabbitMQ publish failures must not roll back an already-committed payment status update.
    private void publishSafely(Runnable publish, UUID paymentId) {
        try {
            publish.run();
        } catch (Exception e) {
            log.error("Failed to publish payment event [paymentId={}]", paymentId, e);
        }
    }
}
