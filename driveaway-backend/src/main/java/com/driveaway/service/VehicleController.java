package com.driveaway.controller;

import com.driveaway.entity.Vehicle;
import com.driveaway.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * VehicleController — REST endpoints for vehicle data.
 *
 * Primarily owned by Person 2, but we add this minimal version
 * so the frontend dropdown can load real vehicles from MongoDB.
 *
 * If Person 2 already has a VehicleController, ADD the /available
 * endpoint to theirs rather than using this whole file.
 *
 * Endpoints:
 *   GET /api/vehicles          → all vehicles
 *   GET /api/vehicles/available → only available vehicles (for booking dropdown)
 *   GET /api/vehicles/{id}     → single vehicle by id
 */
@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
public class VehicleController {

    @Autowired
    private VehicleRepository vehicleRepository;

    /**
     * Get ALL vehicles (for admin/staff viewing).
     */
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleRepository.findAll());
    }

    /**
     * Get only AVAILABLE vehicles — used by customer booking dropdown.
     * Filters where available=true AND status="AVAILABLE"
     */
    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        List<Vehicle> available = vehicleRepository.findAll()
                .stream()
                .filter(v -> v.isAvailable() && "AVAILABLE".equalsIgnoreCase(v.getStatus()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(available);
    }

    /**
     * Get single vehicle by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable String id) {
        return vehicleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update vehicle availability (called internally by BookingService).
     * Person 3's BookingService already updates via VehicleRepository directly,
     * so this endpoint is optional — useful for manual admin updates.
     */
    @PutMapping("/{id}/availability")
    public ResponseEntity<?> updateAvailability(@PathVariable String id,
                                                 @RequestParam boolean available,
                                                 @RequestParam String status) {
        return vehicleRepository.findById(id).map(vehicle -> {
            vehicle.setAvailable(available);
            vehicle.setStatus(status);
            return ResponseEntity.ok(vehicleRepository.save(vehicle));
        }).orElse(ResponseEntity.notFound().build());
    }
}