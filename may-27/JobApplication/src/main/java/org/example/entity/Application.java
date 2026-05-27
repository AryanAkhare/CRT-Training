package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate applicationDate;
    private String status;
    private Long studentId;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;
}
