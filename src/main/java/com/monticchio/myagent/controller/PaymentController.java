package com.monticchio.myagent.controller;

import com.monticchio.myagent.service.PaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public record CheckoutResponse(String url) {}

    @PostMapping("/checkout")
    public CheckoutResponse checkout(Authentication authentication) {
        String url = paymentService.createCheckoutSession(authentication.getName());
        return new CheckoutResponse(url);
    }

    @PostMapping("/webhook")
    public void webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        paymentService.handleWebhookEvent(payload, signature);
    }
}
