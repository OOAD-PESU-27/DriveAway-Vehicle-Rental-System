package com.driveaway.repository;

import com.driveaway.entity.Holiday;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HolidayRepository extends MongoRepository<Holiday, String> {
}