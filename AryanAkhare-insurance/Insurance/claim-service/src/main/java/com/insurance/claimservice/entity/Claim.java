package com.insurance.claimservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long policyId;
    private Double claimAmount;
    private String description;
    private String status; // SUBMITTED, VERIFIED, APPROVED, REJECTED
}
