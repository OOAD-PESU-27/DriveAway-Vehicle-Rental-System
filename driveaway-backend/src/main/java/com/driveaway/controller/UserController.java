package com.driveaway.controller;

import com.driveaway.entity.User;
import com.driveaway.repository.UserRepository;
import com.driveaway.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Get user profile by ID
     * GET /user/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update user profile
     * PUT /user/{id}
     * Body: { "name": "...", "phone": "..." }
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable String id,
            @RequestBody Map<String, String> updates) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(user -> {
                    if (updates.containsKey("name") && updates.get("name") != null
                            && !updates.get("name").isBlank()) {
                        user.setName(updates.get("name"));
                    }
                    if (updates.containsKey("phone")) {
                        user.setPhone(updates.get("phone"));
                    }
                    // Email change is intentionally not supported here to avoid
                    // breaking auth; add a dedicated change-email flow if needed.
                    User saved = userRepository.save(user);
                    auditLogService.logUserAction("PROFILE_UPDATED", saved.getId(), saved.getId(),
                            "User profile updated");
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
