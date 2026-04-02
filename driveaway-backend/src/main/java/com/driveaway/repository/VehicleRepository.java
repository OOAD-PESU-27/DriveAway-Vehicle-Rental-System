package com.driveaway.repository;

import com.driveaway.entity.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * VehicleRepository - Handles database operations for vehicles
 * GRASP: Repository Pattern - Abstracts database operations
 */
@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {

    List<Vehicle> findByAvailableTrue();

    List<Vehicle> findByVehicleType(String vehicleType);

    List<Vehicle> findByAvailableTrueAndVehicleType(String vehicleType);

    List<Vehicle> findByStatus(String status);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
}