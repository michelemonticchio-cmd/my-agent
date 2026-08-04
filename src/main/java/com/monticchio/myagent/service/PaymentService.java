package com.monticchio.myagent.service;

import com.monticchio.myagent.exception.PaymentException;
import com.monticchio.myagent.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final String webhookSecret;
    private final UserRepository userRepository;
    private final QuotaService quotaService;

    public PaymentService(
            @Value("${stripe.secret.key}") String secretKey,
            @Value("${stripe.webhook.secret}") String webhookSecret,
            UserRepository userRepository,
            QuotaService quotaService) {
        Stripe.apiKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.userRepository = userRepository;
        this.quotaService = quotaService;
    }

    public String createCheckoutSession(String username) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(username)
                .setSuccessUrl("http://localhost:8080/?payment=success")
                .setCancelUrl("http://localhost:8080/?payment=cancelled")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(100L)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Unlimited requests until quota window resets")
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            throw new PaymentException("Failed to create Stripe checkout session", e);
        }
    }

    public void handleWebhookEvent(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new PaymentException("Invalid Stripe webhook signature", e);
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) {
            return;
        }

        Session session = (Session) deserializer.getObject().get();
        String username = session.getClientReferenceId();
        if (username == null) {
            return;
        }

        userRepository.findByUsername(username).ifPresent(quotaService::grantUnlimitedForCurrentWindow);
    }
}
