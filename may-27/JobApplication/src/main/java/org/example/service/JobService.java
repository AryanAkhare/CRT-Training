package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.JobDTO;
import org.example.entity.Job;
import org.example.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    public JobDTO createJob(JobDTO jobDTO) {
        Job job = new Job();
        job.setCompanyName(jobDTO.getCompanyName());
        job.setRole(jobDTO.getRole());
        job.setPackageAmount(jobDTO.getPackageAmount());
        job.setLocation(jobDTO.getLocation());

        job = jobRepository.save(job);
        jobDTO.setId(job.getId());
        return jobDTO;
    }

    public List<JobDTO> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private JobDTO mapToDTO(Job job) {
        JobDTO dto = new JobDTO();
        dto.setId(job.getId());
        dto.setCompanyName(job.getCompanyName());
        dto.setRole(job.getRole());
        dto.setPackageAmount(job.getPackageAmount());
        dto.setLocation(job.getLocation());
        return dto;
    }
}
