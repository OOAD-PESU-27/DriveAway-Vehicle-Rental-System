package com.driveaway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — Updated to also permit /api/bookings/maintenance/** and /api/bookings/damages/**
 * so StaffController can call these endpoints without auth tokens.
 *
 * For a production app you would add JWT here.
 * For demo/academic: all /auth/** and /api/** are open.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Open endpoints — no login token required
                .requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/api/bookings/**",    // ALL booking endpoints (create, handover, return, etc.)
                    "/api/vehicles/**",    // Person 2's vehicle endpoints
                    "/license/**",         // License endpoints
                    "/user/**"             // User endpoints
                ).permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
