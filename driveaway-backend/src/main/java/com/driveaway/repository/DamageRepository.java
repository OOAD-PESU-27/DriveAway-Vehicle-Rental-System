package com.driveaway.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.driveaway.entity.Damage;

/**
 * DamageRepository - Handles database operations for damage records
 * GRASP: Repository Pattern - Abstracts database operations
 */
@Repository
public interface DamageRepository extends MongoRepository<Damage, String> {

    List<Damage> findByVehicleId(String vehicleId);

    List<Damage> findByBookingId(String bookingId);

    List<Damage> findBySeverity(String severity);

    List<Damage> findByStatus(String status);
}
