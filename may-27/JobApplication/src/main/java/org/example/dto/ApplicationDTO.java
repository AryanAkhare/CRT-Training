package org.example.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ApplicationDTO {
    private Long id;
    private LocalDate applicationDate;
    private String status;
    private Long studentId;
    private JobDTO job;
}
