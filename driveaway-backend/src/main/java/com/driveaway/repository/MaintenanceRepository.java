package com.driveaway.repository;

import com.driveaway.entity.MaintenanceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaintenanceRepository extends MongoRepository<MaintenanceRecord, String> {
    List<MaintenanceRecord> findByVehicleId(String vehicleId);
    List<MaintenanceRecord> findByStatus(String status);
}
