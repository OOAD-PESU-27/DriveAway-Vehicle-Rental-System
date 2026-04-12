package com.driveaway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. Keeps your existing password encoder
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Adds the rules to open up the endpoints for BOTH you and your friend
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disables CSRF so your JavaFX app can send POST requests
            .authorizeHttpRequests(auth -> auth
                // 👇 VIP list updated! Includes your Auth/License AND your friend's Vehicles/Dates
                .requestMatchers("/auth/**", "/license/**", "/vehicles/**", "/dates/**", "/error").permitAll() 
                .anyRequest().authenticated() // Locks down everything else
            );
        
        return http.build();
    }
}