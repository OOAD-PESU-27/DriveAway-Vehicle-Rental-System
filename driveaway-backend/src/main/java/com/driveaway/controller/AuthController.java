package com.driveaway.controller;

import com.driveaway.dto.LoginRequest;
import com.driveaway.dto.RegisterRequest;
import com.driveaway.entity.User;
import com.driveaway.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public User login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }
}