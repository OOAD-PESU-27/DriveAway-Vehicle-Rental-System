package com.driveaway.controller;

import com.driveaway.dto.BookingRequest;
import com.driveaway.entity.Booking;
import com.driveaway.service.BookingService;
import com.driveaway.exception.PaymentException;
import com.driveaway.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * BookingController - Handles HTTP requests related to bookings
 * GRASP: Controller Pattern - Handles system events and user requests
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@CrossOrigin
public class BookingController {

    private final BookingService bookingService;

    /**
     * Create a new booking
     * POST /api/v1/bookings
     */
    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader(value = "X-User-ID", required = true) String userId) {
        try {
            Booking booking = bookingService.createBooking(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get booking by ID
     * GET /api/v1/bookings/{bookingId}
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingById(@PathVariable String bookingId) {
        try {
            return ResponseEntity.ok(bookingService.getBookingById(bookingId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get bookings for a user
     * GET /api/v1/bookings/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getBookingsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    /**
     * Get all bookings (admin)
     * GET /api/v1/bookings
     */
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    /**
     * Cancel a booking
     * POST /api/v1/bookings/{bookingId}/cancel
     */
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "X-User-ID", required = true) String userId) {
        try {
            Booking booking = bookingService.cancelBooking(bookingId, userId);
            return ResponseEntity.ok(booking);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Complete a booking (vehicle returned)
     * POST /api/v1/bookings/{bookingId}/complete
     */
    @PostMapping("/{bookingId}/complete")
    public ResponseEntity<?> completeBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "X-Staff-ID", required = true) String staffId) {
        try {
            Booking booking = bookingService.completeBooking(bookingId, staffId);
            return ResponseEntity.ok(booking);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}