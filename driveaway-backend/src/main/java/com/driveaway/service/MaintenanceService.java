package com.driveaway.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.driveaway.entity.MaintenanceRecord;
import com.driveaway.repository.MaintenanceRepository;
import com.driveaway.repository.VehicleRepository;

@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;

    @Autowired
    public MaintenanceService(MaintenanceRepository maintenanceRepository, VehicleRepository vehicleRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public MaintenanceRecord scheduleMaintenance(String vehicleId, String reason) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicleId(vehicleId);
        record.setReason(reason);
        record.setScheduledDate(LocalDate.now().plusDays(1));
        record.setStatus("SCHEDULED");
        markVehicleUnderMaintenance(vehicleId);
        return maintenanceRepository.save(record);
    }

    public MaintenanceRecord completeMaintenance(String maintenanceId) {
        MaintenanceRecord record = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance not found."));

        record.setStatus("COMPLETED");
        record.setCompletedDate(LocalDate.now());
        MaintenanceRecord saved = maintenanceRepository.save(record);
        restoreVehicle(record.getVehicleId());
        return saved;
    }

    public List<MaintenanceRecord> getAll() { return maintenanceRepository.findAll(); }
    public List<MaintenanceRecord> getByVehicle(String vehicleId) { return maintenanceRepository.findByVehicleId(vehicleId); }
    public List<MaintenanceRecord> getScheduled() { return maintenanceRepository.findByStatus("SCHEDULED"); }

    private void markVehicleUnderMaintenance(String vehicleId) {
        vehicleRepository.findById(vehicleId).ifPresent(vehicle -> {
            vehicle.setAvailable(false);
            vehicle.setStatus("MAINTENANCE");
            vehicleRepository.save(vehicle);
        });
    }

    private void restoreVehicle(String vehicleId) {
        vehicleRepository.findById(vehicleId).ifPresent(vehicle -> {
            vehicle.setAvailable(true);
            vehicle.setStatus("AVAILABLE");
            vehicleRepository.save(vehicle);
        });
    }
}
