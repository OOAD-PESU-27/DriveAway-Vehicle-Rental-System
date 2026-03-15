package com.driveaway.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(
            "mongodb+srv://DriveAway_01:driveaway%401@driveaway.rm74yrq.mongodb.net/driveaway?retryWrites=true&w=majority"
        );
    }
}