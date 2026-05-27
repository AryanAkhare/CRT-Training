package com.insurance.claimservice.controller;

import com.insurance.claimservice.entity.Claim;
import com.insurance.claimservice.repository.ClaimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestTemplate;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    @Autowired
    private ClaimRepository claimRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/{id}/verify")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "policyService", fallbackMethod = "verifyClaimFallback")
    public ResponseEntity<String> verifyClaim(@PathVariable Long id) {
        Claim claim = claimRepository.findById(id).orElseThrow(() -> new RuntimeException("Claim not found"));
        String policyUrl = "http://policy-service/api/policies/" + claim.getPolicyId();
        ResponseEntity<Object> response = restTemplate.getForEntity(policyUrl, Object.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            claim.setStatus("VERIFIED");
            claimRepository.save(claim);
            return ResponseEntity.ok("Claim Verified Successfully");
        }
        return ResponseEntity.badRequest().body("Policy validation failed");
    }

    public ResponseEntity<String> verifyClaimFallback(Long id, Throwable t) {
        return ResponseEntity.status(503).body("Policy Service is currently unavailable. Cannot verify claim right now.");
    }

    @PostMapping
    public Claim createClaim(@RequestBody Claim claim) {
        if (claim.getStatus() == null) {
            claim.setStatus("SUBMITTED");
        }
        return claimRepository.save(claim);
    }

    @GetMapping
    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Claim> getClaim(@PathVariable Long id) {
        return claimRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
