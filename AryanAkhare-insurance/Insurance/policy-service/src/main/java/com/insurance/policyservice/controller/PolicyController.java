package com.insurance.policyservice.controller;

import com.insurance.policyservice.entity.Policy;
import com.insurance.policyservice.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestTemplate;
import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    @Autowired
    private PolicyRepository policyRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/{id}/approve")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "customerService", fallbackMethod = "approvePolicyFallback")
    public ResponseEntity<String> approvePolicy(@PathVariable Long id) {
        Policy policy = policyRepository.findById(id).orElseThrow(() -> new RuntimeException("Policy not found"));
        String customerUrl = "http://customer-service/api/customers/" + policy.getCustomerId();
        ResponseEntity<Object> response = restTemplate.getForEntity(customerUrl, Object.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            policy.setStatus("APPROVED");
            policyRepository.save(policy);
            return ResponseEntity.ok("Policy Approved Successfully");
        }
        return ResponseEntity.badRequest().body("Customer validation failed");
    }

    public ResponseEntity<String> approvePolicyFallback(Long id, Throwable t) {
        return ResponseEntity.status(503).body("Customer Service is currently unavailable. Cannot approve policy right now.");
    }

    @PostMapping
    public Policy createPolicy(@RequestBody Policy policy) {
        if (policy.getStatus() == null) {
            policy.setStatus("PENDING");
        }
        return policyRepository.save(policy);
    }

    @GetMapping
    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Policy> getPolicy(@PathVariable Long id) {
        return policyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public List<Policy> getPoliciesByCustomerId(@PathVariable Long customerId) {
        return policyRepository.findByCustomerId(customerId);
    }
}
