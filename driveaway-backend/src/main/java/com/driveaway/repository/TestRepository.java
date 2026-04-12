package com.driveaway.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.driveaway.entity.Test;

public interface TestRepository extends MongoRepository<Test, String> {

}