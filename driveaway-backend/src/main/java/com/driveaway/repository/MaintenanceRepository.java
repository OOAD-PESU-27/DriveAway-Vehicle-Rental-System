package com.driveaway.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.driveaway.entity.MaintenanceRecord;

@Repository
public interface MaintenanceRepository extends MongoRepository<MaintenanceRecord, String> {

    List<MaintenanceRecord> findByVehicleId(String vehicleId);

    List<MaintenanceRecord> findByStatus(String status);

    List<MaintenanceRecord> findByVehicleIdAndStatus(String vehicleId, String status);
}