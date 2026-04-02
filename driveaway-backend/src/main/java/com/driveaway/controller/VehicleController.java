package com.driveaway.controller;

import com.driveaway.entity.Vehicle;
import com.driveaway.service.VehicleService;
import com.driveaway.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * VehicleController - Handles HTTP requests related to vehicles
 * GRASP: Controller Pattern - Handles system events and user requests
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * Get all vehicles
     * GET /api/v1/vehicles
     */
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    /**
     * Get available vehicles
     * GET /api/v1/vehicles/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(vehicleService.getAvailableVehiclesByType(type));
        }
        return ResponseEntity.ok(vehicleService.getAvailableVehicles());
    }

    /**
     * Get vehicle by ID
     * GET /api/v1/vehicles/{vehicleId}
     */
    @GetMapping("/{vehicleId}")
    public ResponseEntity<?> getVehicleById(@PathVariable String vehicleId) {
        try {
            return ResponseEntity.ok(vehicleService.getVehicleById(vehicleId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Add a new vehicle (admin)
     * POST /api/v1/vehicles
     */
    @PostMapping
    public ResponseEntity<Vehicle> addVehicle(
            @RequestBody Vehicle vehicle,
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        Vehicle saved = vehicleService.addVehicle(vehicle, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Update vehicle (admin)
     * PUT /api/v1/vehicles/{vehicleId}
     */
    @PutMapping("/{vehicleId}")
    public ResponseEntity<?> updateVehicle(
            @PathVariable String vehicleId,
            @RequestBody Vehicle vehicle,
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            return ResponseEntity.ok(vehicleService.updateVehicle(vehicleId, vehicle, adminId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete vehicle (admin)
     * DELETE /api/v1/vehicles/{vehicleId}
     */
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<?> deleteVehicle(
            @PathVariable String vehicleId,
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            vehicleService.deleteVehicle(vehicleId, adminId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}