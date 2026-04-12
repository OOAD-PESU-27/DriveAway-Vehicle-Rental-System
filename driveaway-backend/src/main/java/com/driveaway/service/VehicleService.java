package com.driveaway.service;

import com.driveaway.entity.Vehicle;
import com.driveaway.repository.VehicleRepository;
import com.driveaway.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * VehicleService - Contains business logic for vehicle management
 * GRASP: Information Expert - Handles vehicle-related business logic
 * SOLID: SRP - Only handles vehicle operations
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final AuditLogService auditLogService;

    /**
     * Get all vehicles
     */
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    /**
     * Get all available vehicles
     */
    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByAvailableTrue();
    }

    /**
     * Get available vehicles by type
     */
    public List<Vehicle> getAvailableVehiclesByType(String vehicleType) {
        return vehicleRepository.findByAvailableTrueAndVehicleType(vehicleType);
    }

    /**
     * Get vehicle by ID
     */
    public Vehicle getVehicleById(String vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));
    }

    /**
     * Add a new vehicle
     */
    public Vehicle addVehicle(Vehicle vehicle, String adminId) {
        vehicle.setAvailable(true);
        vehicle.setStatus("AVAILABLE");
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        auditLogService.logPaymentAction("VEHICLE_ADDED", savedVehicle.getId(), adminId,
                "Vehicle added: " + vehicle.getBrand() + " " + vehicle.getModel());
        return savedVehicle;
    }

    /**
     * Update vehicle details
     */
    public Vehicle updateVehicle(String vehicleId, Vehicle updatedVehicle, String adminId) {
        Vehicle existing = getVehicleById(vehicleId);
        existing.setBrand(updatedVehicle.getBrand());
        existing.setModel(updatedVehicle.getModel());
        existing.setVehicleType(updatedVehicle.getVehicleType());
        existing.setPricePerDay(updatedVehicle.getPricePerDay());
        Vehicle saved = vehicleRepository.save(existing);
        auditLogService.logPaymentAction("VEHICLE_UPDATED", vehicleId, adminId,
                "Vehicle updated: " + vehicleId);
        return saved;
    }

    /**
     * Mark vehicle as unavailable (booked)
     */
    public void markVehicleAsBooked(String vehicleId) {
        Vehicle vehicle = getVehicleById(vehicleId);
        vehicle.setAvailable(false);
        vehicle.setStatus("BOOKED");
        vehicleRepository.save(vehicle);
    }

    /**
     * Mark vehicle as available (booking ended or cancelled)
     */
    public void markVehicleAsAvailable(String vehicleId) {
        Vehicle vehicle = getVehicleById(vehicleId);
        vehicle.setAvailable(true);
        vehicle.setStatus("AVAILABLE");
        vehicleRepository.save(vehicle);
    }

    /**
     * Mark vehicle as under maintenance
     */
    public void markVehicleUnderMaintenance(String vehicleId) {
        Vehicle vehicle = getVehicleById(vehicleId);
        vehicle.setAvailable(false);
        vehicle.setStatus("MAINTENANCE");
        vehicleRepository.save(vehicle);
    }

    /**
     * Delete a vehicle
     */
    public void deleteVehicle(String vehicleId, String adminId) {
        Vehicle vehicle = getVehicleById(vehicleId);
        vehicleRepository.delete(vehicle);
        auditLogService.logPaymentAction("VEHICLE_DELETED", vehicleId, adminId,
                "Vehicle deleted: " + vehicleId);
    }
}