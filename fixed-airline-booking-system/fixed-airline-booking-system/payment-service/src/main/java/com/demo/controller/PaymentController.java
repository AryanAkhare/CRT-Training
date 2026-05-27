package com.demo.controller;

import com.demo.model.Payment;
import com.demo.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/payments/pay")
    public String pay() {
        Payment payment = new Payment(UUID.randomUUID().toString(), 150.0, "SUCCESS");
        paymentRepository.save(payment);
        return "PAYMENT SUCCESS";
    }

    @GetMapping("/payments")
    public List<Payment> getPayments() {
        return paymentRepository.findAll();
    }
}
