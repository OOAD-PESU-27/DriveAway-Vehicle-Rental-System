package com.driveaway.controller;

import com.driveaway.entity.Notification;
import com.driveaway.service.NotificationService;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * NotificationController - Handles HTTP requests related to notifications
 * GRASP: Controller Pattern - Handles system events and user requests
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@CrossOrigin
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications (admin)
     * GET /api/v1/notifications
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications(
            @RequestHeader(value = "X-Admin-ID", required = false) String adminId) {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    /**
     * Get all notifications for a user
     * GET /api/v1/notifications/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotificationsForUser(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }

    /**
     * Get unread notifications for a user
     * GET /api/v1/notifications/user/{userId}/unread
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotificationsForUser(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationsForUser(userId));
    }

    /**
     * Mark a notification as read
     * POST /api/v1/notifications/{notificationId}/read
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String notificationId) {
        try {
            notificationService.markAsRead(notificationId);
            return ResponseEntity.ok("Notification marked as read");
        } catch (PaymentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
