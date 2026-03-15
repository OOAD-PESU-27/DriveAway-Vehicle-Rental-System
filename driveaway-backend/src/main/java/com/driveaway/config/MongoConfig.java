package com.driveaway.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient() {

        Dotenv dotenv = Dotenv.load();
        String uri = dotenv.get("MONGO_URI");

        return MongoClients.create(uri);
    }
}