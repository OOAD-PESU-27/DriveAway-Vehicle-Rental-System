package com.driveaway.service;

import com.driveaway.dto.VehicleResponse;
import com.driveaway.entity.Vehicle;
import com.driveaway.exception.ResourceNotFoundException;
import com.driveaway.repository.HolidayRepository;
import com.driveaway.repository.VehicleRepository;
import com.driveaway.service.pricing.PricingFactory;
import com.driveaway.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
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
    private final HolidayRepository holidayRepository;
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

    // ===================== NEW FEATURE FOR DYNAMIC PRICING =====================

    /**
     * Vehicle search with holiday/weekend/weekday dynamic pricing and breakdown.
     */
    public List<VehicleResponse> getAvailableVehiclesWithPrice(List<LocalDate> dates) {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        List<String> holidays = holidayRepository.findAll()
                .stream()
                .map(h -> h.getDate())
                .toList();

        int holidayCount = DateService.countHolidays(dates, holidays);
        int weekendCount = DateService.countWeekends(dates);
        int weekdayCount = DateService.countWeekdays(dates, holidays);

        PricingStrategy strategy = PricingFactory.getStrategy(holidayCount, weekendCount);

        List<VehicleResponse> responseList = new ArrayList<>();

        for (Vehicle v : vehicles) {
            double basePrice = v.getPricePerDay();
            double total = strategy.calculate(basePrice, dates, holidays);
            String breakdown =
                    "Base: " + weekdayCount + " × ₹" + basePrice + "\n" +
                    "Weekend: " + weekendCount + " × ₹" + (basePrice * 1.3) + "\n" +
                    "Holiday: " + holidayCount + " × ₹" + (basePrice * 1.5);

            VehicleResponse res = new VehicleResponse();
            res.setId(v.getId());
            res.setName(v.getBrand() + " " + v.getModel());
            res.setSeatingCapacity(v.getSeatingCapacity());
            res.setPricePerDay(basePrice);
            res.setWeekendPricePerDay(basePrice * 1.3);
            res.setHolidayPricePerDay(basePrice * 1.5);

            res.setTotalPrice(total);
            res.setPriceBreakdown(breakdown);

            responseList.add(res);
        }

        return responseList;
    }
}