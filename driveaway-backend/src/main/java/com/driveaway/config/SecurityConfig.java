package com.driveaway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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

    // 2. Adds the rules to open up the login and register endpoints
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disables CSRF so your JavaFX app can send POST requests
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()          // auth endpoints
                .requestMatchers("/api/v1/**").permitAll()        // all API v1 endpoints
                .requestMatchers("/user/**").permitAll()          // user profile endpoint
                .requestMatchers("/license/**").permitAll()       // license endpoint
                // 👇 THE FIX: Person 4's endpoints are back on the VIP list!
                .requestMatchers("/vehicles/**", "/dates/**").permitAll() 
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}