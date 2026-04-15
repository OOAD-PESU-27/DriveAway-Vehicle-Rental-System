package com.driveaway.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.driveaway.entity.MaintenanceRecord;
import com.driveaway.exception.ResourceNotFoundException;
import com.driveaway.repository.MaintenanceRepository;

import lombok.RequiredArgsConstructor;

/**
 * MaintenanceService - Contains business logic for vehicle maintenance
 * GRASP: Information Expert - Handles maintenance-related business logic
 * SOLID: SRP - Only handles maintenance operations
 */
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final VehicleService vehicleService;
    private final AuditLogService auditLogService;

    /**
     * Schedule a maintenance task for a vehicle
     */
    public MaintenanceRecord scheduleMaintenance(String vehicleId, String staffId,
                                                  String maintenanceType, String description,
                                                  double cost, LocalDate scheduledDate) {
        vehicleService.getVehicleById(vehicleId); // validates vehicle exists

        MaintenanceRecord record = new MaintenanceRecord(
                vehicleId, staffId, maintenanceType, description, cost, scheduledDate);

        MaintenanceRecord saved = maintenanceRepository.save(record);

        vehicleService.markVehicleUnderMaintenance(vehicleId);

        auditLogService.logPaymentAction("MAINTENANCE_SCHEDULED", saved.getId(), staffId,
                "Maintenance scheduled for vehicle: " + vehicleId + " type: " + maintenanceType);

        return saved;
    }

    /**
     * Mark maintenance as completed
     */
    public MaintenanceRecord completeMaintenance(String recordId, String staffId) {
        MaintenanceRecord record = maintenanceRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRecord", "id", recordId));

        record.setStatus("COMPLETED");
        record.setCompletedDate(LocalDate.now());
        MaintenanceRecord saved = maintenanceRepository.save(record);

        vehicleService.markVehicleAsAvailable(record.getVehicleId());

        auditLogService.logPaymentAction("MAINTENANCE_COMPLETED", recordId, staffId,
                "Maintenance completed for vehicle: " + record.getVehicleId());

        return saved;
    }

    /**
     * Get all maintenance records for a vehicle
     */
    public List<MaintenanceRecord> getMaintenanceByVehicle(String vehicleId) {
        return maintenanceRepository.findByVehicleId(vehicleId);
    }

    /**
     * Get all pending/in-progress maintenance records
     */
    public List<MaintenanceRecord> getPendingMaintenance() {
        return maintenanceRepository.findByStatus("SCHEDULED");
    }
}