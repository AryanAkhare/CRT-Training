package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ApplicationDTO;
import org.example.dto.ApplyRequest;
import org.example.service.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyForJob(@RequestBody ApplyRequest applyRequest) {
        try {
            ApplicationDTO application = applicationService.applyForJob(applyRequest);
            return ResponseEntity.ok(application);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<ApplicationDTO>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }
}
