package com.insurance.paymentservice.controller;

import com.insurance.paymentservice.entity.Payment;
import com.insurance.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/process")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "policyService", fallbackMethod = "processPaymentFallback")
    public ResponseEntity<?> processPaymentWithVerification(@RequestBody Payment payment) {
        String policyUrl = "http://policy-service/api/policies/" + payment.getPolicyId();
        ResponseEntity<Object> response = restTemplate.getForEntity(policyUrl, Object.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            payment.setPaymentDate(LocalDateTime.now());
            payment.setStatus("SUCCESS");
            return ResponseEntity.ok(paymentRepository.save(payment));
        }
        return ResponseEntity.badRequest().body("Policy validation failed");
    }

    public ResponseEntity<?> processPaymentFallback(Payment payment, Throwable t) {
        return ResponseEntity.status(503).body("Policy Service is currently unavailable. Cannot process payment right now.");
    }



    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
