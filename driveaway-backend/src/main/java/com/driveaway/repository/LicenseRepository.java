package com.driveaway.repository;

import com.driveaway.entity.License;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LicenseRepository extends MongoRepository<License, String> {
    Optional<License> findByUserId(String userId);
}