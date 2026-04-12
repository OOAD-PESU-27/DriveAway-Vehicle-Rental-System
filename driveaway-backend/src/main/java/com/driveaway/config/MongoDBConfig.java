package com.driveaway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB Configuration - Enables MongoDB repositories and configures MongoDB
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.driveaway.repository")
public class MongoDBConfig {
    
    // MongoDB is configured via application.properties
    // spring.data.mongodb.uri=${MONGO_URI}
    
    // Additional configurations can be added here if needed
}