package com.driveaway.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.driveaway.entity.Vehicle;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {
}