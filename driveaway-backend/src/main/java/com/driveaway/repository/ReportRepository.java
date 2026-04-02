package com.driveaway.repository;

public package com.driveaway.repository;

import com.driveaway.entity.Report;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ReportRepository - Handles database operations for reports
 * GRASP: Repository Pattern - Abstracts database operations
 */
@Repository
public interface ReportRepository extends MongoRepository<Report, String> {
    
    List<Report> findByReportType(String reportType);
    
    List<Report> findByGeneratedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    Optional<Report> findLatestByReportType(String reportType);
} {
    
}
