package com.driveaway.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.driveaway.dto.InspectionRequest;
import com.driveaway.entity.Damage;
import com.driveaway.repository.DamageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DamageService {

    private final DamageRepository damageRepository;

    private static final double PENALTY_LOW = 2000.0;
    private static final double PENALTY_MEDIUM = 6000.0;
    private static final double PENALTY_HIGH = 15000.0;


    public Damage logDamage(InspectionRequest request) {

        if (!request.isHasDamage()) {
            return null;
        }

        double penalty = calculatePenalty(request.getDamageSeverity());

        Damage damage = new Damage();

        damage.setBookingId(request.getBookingId());
        damage.setVehicleId(request.getVehicleId());
        damage.setDescription(request.getDamageDescription());
        damage.setSeverity(request.getDamageSeverity().toUpperCase());
        damage.setEstimatedRepairCost(penalty);
        damage.setStatus("REPORTED");
        damage.setReportedAt(LocalDateTime.now());
        damage.setReportedBy("STAFF");

        return damageRepository.save(damage);
    }


    public List<Damage> getDamagesByBooking(String bookingId) {
        return damageRepository.findByBookingId(bookingId);
    }


    public List<Damage> getDamagesByVehicle(String vehicleId) {
        return damageRepository.findByVehicleId(vehicleId);
    }


    private double calculatePenalty(String severity) {

        if (severity == null) return 0;

        switch (severity.toUpperCase()) {

            case "LOW":
                return PENALTY_LOW;

            case "MEDIUM":
                return PENALTY_MEDIUM;

            case "HIGH":
                return PENALTY_HIGH;

            default:
                return 0;
        }
    }
}