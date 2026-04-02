package com.driveaway.repository;

import com.driveaway.entity.MaintenanceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * MaintenanceRepository - Handles database operations for maintenance records
 * GRASP: Repository Pattern - Abstracts database operations
 */
@Repository
public interface MaintenanceRepository extends MongoRepository<MaintenanceRecord, String> {

    List<MaintenanceRecord> findByVehicleId(String vehicleId);

    List<MaintenanceRecord> findByStatus(String status);

    List<MaintenanceRecord> findByVehicleIdAndStatus(String vehicleId, String status);

    List<MaintenanceRecord> findByPerformedBy(String performedBy);
}