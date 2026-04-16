package com.driveaway.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.driveaway.dto.VehicleResponse;
import com.driveaway.entity.Vehicle;
import com.driveaway.exception.ResourceNotFoundException;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.HolidayRepository;
import com.driveaway.repository.VehicleRepository;
import com.driveaway.service.pricing.PricingFactory;
import com.driveaway.service.pricing.PricingStrategy;

import lombok.RequiredArgsConstructor;

/**
 * VehicleService - Contains business logic for vehicle management
 * GRASP: Information Expert - Handles vehicle-related business logic
 * SOLID: SRP - Only handles vehicle operations
 *
 * FIX: getAvailableVehiclesWithPrice() now uses BookingRepository to check
 * real date-range conflicts instead of DateSelectionRepository, which was
 * never populated by the normal booking flow. This ensures the "available"
 * flag shown on the vehicle catalog correctly reflects actual bookings.
 */

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final HolidayRepository holidayRepository;
    private final AuditLogService auditLogService;
    private final BookingRepository bookingRepository; // FIX: injected to check real booking conflicts

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

    // ===================== DYNAMIC PRICING SEARCH =====================

    /**
     * Vehicle search with holiday/weekend/weekday dynamic pricing and breakdown.
     *
     * FIX: Now checks real booking conflicts using BookingRepository instead of
     * DateSelectionRepository. DateSelectionRepository was always empty for bookings
     * made through the normal flow, so all vehicles incorrectly showed as available.
     */
    public List<VehicleResponse> getAvailableVehiclesWithPrice(List<LocalDate> dates) {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        LocalDate startDate = dates.get(0);
        LocalDate endDate = dates.get(dates.size() - 1);

        System.out.println("📅 Date range: " + startDate + " to " + endDate);
        System.out.println("📦 Total vehicles in DB: " + vehicles.size());

        List<String> holidays = holidayRepository.findAll()
                .stream()
                .map(h -> h.getDate())
                .toList();

        int holidayCount = DateService.countHolidays(dates, holidays);
        int weekendCount = DateService.countWeekends(dates);
        int weekdayCount = DateService.countWeekdays(dates, holidays);

        System.out.println("📊 Days — weekday:" + weekdayCount +
                        " weekend:" + weekendCount +
                        " holiday:" + holidayCount);

        PricingStrategy strategy = PricingFactory.getStrategy(holidayCount, weekendCount);

        List<VehicleResponse> responseList = new ArrayList<>();

        for (Vehicle v : vehicles) {
            System.out.println("🚗 Checking vehicle: " + v.getId());

            // FIX: Use BookingRepository to check for CONFIRMED booking overlaps.
            // Previously used DateSelectionRepository.findOverlappingDates() which
            // was never populated by the normal booking flow, so isAvailable was
            // always true regardless of existing bookings.
            boolean isAvailable = !bookingRepository
                    .existsByVehicleIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            v.getId(), "ACTIVE", endDate, startDate);

            System.out.println("   Available: " + isAvailable);

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
            res.setVehicleType(v.getVehicleType());
            res.setFuelType(v.getFuelType() != null ? v.getFuelType() : "PETROL");
            res.setTransmission(v.getTransmission() != null ? v.getTransmission() : "AUTOMATIC");
            res.setPricePerDay(basePrice);
            res.setWeekendPricePerDay(basePrice * 1.3);
            res.setHolidayPricePerDay(basePrice * 1.5);
            res.setAvailable(isAvailable);
            res.setTotalPrice(total);
            res.setPriceBreakdown(breakdown);

            System.out.println("   ✅ Added: " + v.getBrand() + " " + v.getModel() +
                            " | available=" + isAvailable +
                            " | total=₹" + total);

            responseList.add(res);
        }

        System.out.println("✅ Returning " + responseList.size() + " vehicles");
        return responseList;
    }

}
