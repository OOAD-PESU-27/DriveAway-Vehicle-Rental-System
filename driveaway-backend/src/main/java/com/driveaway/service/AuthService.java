package com.driveaway.service;

import com.driveaway.dto.LoginRequest;
import com.driveaway.dto.RegisterRequest;
import com.driveaway.entity.User;
import com.driveaway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    public String register(RegisterRequest req) {

        if (userRepository.findByEmail(req.email).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setName(req.name);
        user.setEmail(req.email);
        user.setPassword(encoder.encode(req.password));
        user.setPhone(req.phone);
        user.setRole("CUSTOMER");

        userRepository.save(user);

        return "Registered Successfully";
    }

    public User login(LoginRequest req) {

        User user = userRepository.findByEmail(req.email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(req.password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return user;
    }
}