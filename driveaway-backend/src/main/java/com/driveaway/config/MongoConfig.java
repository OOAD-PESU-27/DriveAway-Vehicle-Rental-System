package com.driveaway.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "driveaway_db";

    }

    @Override
    public MongoClient mongoClient() {
        // HARDCODED URL - Bypassing the .env file completely so it cannot fail!
        String uri = "mongodb+srv://DriveAway_01:driveaway%401@driveaway.rm74yrq.mongodb.net/driveaway_db?retryWrites=true&w=majority";
        return MongoClients.create(uri);
    }
}