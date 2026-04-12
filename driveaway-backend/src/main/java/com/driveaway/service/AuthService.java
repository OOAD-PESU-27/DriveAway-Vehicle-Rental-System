package com.driveaway.service;

import com.driveaway.dto.LoginRequest;
import com.driveaway.dto.RegisterRequest;
import com.driveaway.entity.User;
import com.driveaway.exception.DuplicateEmailException;
import com.driveaway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    public String register(RegisterRequest req) {
        if (req.email == null || req.email.isBlank()) {
            throw new IllegalArgumentException("Email address is required");
        }

        String normalizedEmail = req.email.trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("Email already registered");
        }

        User user = new User();
        user.setName(req.name);
        user.setEmail(normalizedEmail);
        user.setPassword(encoder.encode(req.password));
        user.setPhone(req.phone);
        user.setRole("CUSTOMER");

        try {
            userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // Race-condition guard: another request saved the same email between our check and save
            throw new DuplicateEmailException("Email already registered");
        }

        return "Registered Successfully";
    }

    public User login(LoginRequest req) {
        String normalizedEmail = req.email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(req.password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return user;
    }
}