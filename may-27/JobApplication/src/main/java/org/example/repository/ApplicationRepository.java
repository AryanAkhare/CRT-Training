package org.example.repository;

import org.example.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByStudentIdAndJobId(Long studentId, Long jobId);
}
