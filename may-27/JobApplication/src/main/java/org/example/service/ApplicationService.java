package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.ApplicationDTO;
import org.example.dto.ApplyRequest;
import org.example.dto.JobDTO;
import org.example.entity.Application;
import org.example.entity.Job;
import org.example.repository.ApplicationRepository;
import org.example.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public ApplicationDTO applyForJob(ApplyRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        if (applicationRepository.existsByStudentIdAndJobId(request.getStudentId(), request.getJobId())) {
            throw new IllegalStateException("Student has already applied for this job.");
        }

        Application application = new Application();
        application.setApplicationDate(LocalDate.now());
        application.setStatus("APPLIED");
        application.setStudentId(request.getStudentId());
        application.setJob(job);

        application = applicationRepository.save(application);
        return mapToDTO(application);
    }

    public List<ApplicationDTO> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ApplicationDTO mapToDTO(Application application) {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setId(application.getId());
        dto.setApplicationDate(application.getApplicationDate());
        dto.setStatus(application.getStatus());
        dto.setStudentId(application.getStudentId());

        if (application.getJob() != null) {
            JobDTO jobDTO = new JobDTO();
            jobDTO.setId(application.getJob().getId());
            jobDTO.setCompanyName(application.getJob().getCompanyName());
            jobDTO.setRole(application.getJob().getRole());
            jobDTO.setPackageAmount(application.getJob().getPackageAmount());
            jobDTO.setLocation(application.getJob().getLocation());
            dto.setJob(jobDTO);
        }

        return dto;
    }
}
