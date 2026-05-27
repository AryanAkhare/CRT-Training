package com.insurance.policyservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private String policyType;
    private Double premiumAmount;
    private String status; // PENDING, APPROVED, ACTIVE, REJECTED
}
