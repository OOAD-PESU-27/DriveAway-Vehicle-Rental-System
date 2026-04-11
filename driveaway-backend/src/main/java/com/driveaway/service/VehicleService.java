package com.driveaway.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.driveaway.entity.Vehicle;
import com.driveaway.repository.DateSelectionRepository;
import com.driveaway.repository.VehicleRepository;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepo;
    private final DateSelectionRepository dateRepo;

    public VehicleService(VehicleRepository vehicleRepo, DateSelectionRepository dateRepo) {
        this.vehicleRepo = vehicleRepo;
        this.dateRepo = dateRepo;
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepo.findAll();
    }

    public List<Vehicle> getAvailableVehicles(String start, String end) {

        List<Vehicle> vehicles = vehicleRepo.findAll();

        return vehicles.stream()
            .filter(v -> dateRepo
                .findOverlappingDates(v.getId(), start, end)
                .isEmpty()
            )
            .toList();
    }
}