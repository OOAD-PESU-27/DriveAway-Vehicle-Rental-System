package com.driveaway.service;

import com.driveaway.entity.Vehicle;
import com.driveaway.repository.VehicleRepository;
import com.driveaway.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * PricingService - Calculates rental pricing based on vehicle and duration
 * GRASP: Information Expert - Handles pricing-related business logic
 * SOLID: SRP - Only handles pricing calculations
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final VehicleRepository vehicleRepository;

    /**
     * Calculate total price for a booking
     */
    public double calculateTotalPrice(String vehicleId, long rentalDays) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));
        return calculatePrice(vehicle.getPricePerDay(), rentalDays);
    }

    /**
     * Calculate price with base rate and duration
     * Applies a 5% discount for rentals longer than 7 days
     */
    public double calculatePrice(double pricePerDay, long rentalDays) {
        if (rentalDays <= 0) {
            throw new IllegalArgumentException("Rental duration must be at least 1 day");
        }
        double total = pricePerDay * rentalDays;
        if (rentalDays > 7) {
            total = total * 0.95; // 5% discount for weekly+ rentals
        }
        return Math.round(total * 100.0) / 100.0;
    }
}