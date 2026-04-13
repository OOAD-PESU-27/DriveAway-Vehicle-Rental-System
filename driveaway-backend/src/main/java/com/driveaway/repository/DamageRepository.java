package com.driveaway.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.driveaway.entity.Damage;

@Repository
public interface DamageRepository extends MongoRepository<Damage, String> {
    List<Damage> findByBookingId(String bookingId);
    List<Damage> findByVehicleId(String vehicleId);
}
