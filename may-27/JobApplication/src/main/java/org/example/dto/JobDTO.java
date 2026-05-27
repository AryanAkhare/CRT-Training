package org.example.dto;

import lombok.Data;

@Data
public class JobDTO {
    private Long id;
    private String companyName;
    private String role;
    private Double packageAmount;
    private String location;
}
