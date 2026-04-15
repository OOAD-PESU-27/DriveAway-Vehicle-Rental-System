package com.driveaway.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.driveaway.dto.BookingRequest;
import com.driveaway.dto.InspectionRequest;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Damage;
import com.driveaway.exception.PaymentException;
import com.driveaway.exception.ResourceNotFoundException;
import com.driveaway.repository.DamageRepository;
import com.driveaway.service.BookingService;
import com.driveaway.service.MaintenanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@CrossOrigin
public class BookingController {

    private final BookingService bookingService;
    private final DamageRepository damageRepository;
    private final MaintenanceService maintenanceService;


    // =========================================
    // CREATE BOOKING
    // =========================================

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


    // =========================================
    // GET BOOKING BY ID
    // =========================================

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingById(@PathVariable String bookingId) {

        try {

            return ResponseEntity.ok(
                    bookingService.getBookingById(bookingId)
            );

        } catch (ResourceNotFoundException e) {

            return ResponseEntity.notFound().build();
        }
    }


    // =========================================
    // GET BOOKINGS BY USER
    // =========================================

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getBookingsByUser(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                bookingService.getBookingsByUserId(userId)
        );
    }


    // =========================================
    // ADMIN: GET ALL BOOKINGS
    // =========================================

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search) {

        return ResponseEntity.ok(
                bookingService.getBookingsForAdmin(status, search)
        );
    }


    // =========================================
    // CANCEL BOOKING
    // =========================================

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "X-User-ID", required = true) String userId) {

        try {

            Booking booking =
                    bookingService.cancelBooking(bookingId, userId);

            return ResponseEntity.ok(booking);

        } catch (PaymentException e) {

            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (ResourceNotFoundException e) {

            return ResponseEntity.notFound().build();
        }
    }


    // =========================================
    // COMPLETE BOOKING
    // =========================================

    @PostMapping("/{bookingId}/complete")
    public ResponseEntity<?> completeBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "X-Staff-ID", required = true) String staffId) {

        try {

            Booking booking =
                    bookingService.completeBooking(bookingId, staffId);

            return ResponseEntity.ok(booking);

        } catch (PaymentException e) {

            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (ResourceNotFoundException e) {

            return ResponseEntity.notFound().build();
        }
    }


    // =========================================
    // BASIC RETURN ENDPOINT
    // =========================================

    @PostMapping("/{bookingId}/return")
    public ResponseEntity<?> returnBooking(
            @PathVariable String bookingId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Staff-ID",
                    required = false,
                    defaultValue = "STAFF") String staffId) {

        try {

            String damageNotes = "";
            double damageCharge = 0.0;

            if (body != null) {

                Object notes = body.get("damageNotes");

                if (notes != null)
                    damageNotes = notes.toString();

                Object charge = body.get("damageCharge");

                if (charge != null)
                    damageCharge =
                            Double.parseDouble(charge.toString());
            }

            Booking booking =
                    bookingService.processReturn(
                            bookingId,
                            damageNotes,
                            damageCharge,
                            staffId
                    );

            return ResponseEntity.ok(booking);

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // =========================================
    // STAFF INSPECTION (CONNECTED TO UI)
    // =========================================
    @PostMapping("/return-inspection")
    public ResponseEntity<?> returnInspection(
            @RequestBody InspectionRequest request) {

        try {

            double penalty = 0; // declare BEFORE if-block

            if (request.isHasDamage()) {

                penalty = calculatePenalty(request.getDamageSeverity());

                Damage damage = new Damage();

                damage.setBookingId(request.getBookingId());
                damage.setVehicleId(request.getVehicleId());
                damage.setDescription(request.getDamageDescription());
                damage.setSeverity(request.getDamageSeverity());
                damage.setEstimatedRepairCost(penalty);
                damage.setStatus("REPORTED");
                damage.setReportedAt(LocalDateTime.now());

                damageRepository.save(damage);

            maintenanceService.scheduleMaintenance(
                    request.getVehicleId(),
                    "SYSTEM",
                    "REPAIR",
                    "Damage reported during inspection",
                    penalty,
                    java.time.LocalDate.now().plusDays(1)
            );
        }

        bookingService.processReturn(
                request.getBookingId(),
                request.getDamageDescription(),
                penalty,
                "STAFF"
        );

        return ResponseEntity.ok(
                "Inspection completed successfully"
        );

    } catch (Exception e) {

        return ResponseEntity.badRequest().body(
                "Inspection failed: " + e.getMessage()
        );
    }
}

    // =========================================
    // GET BOOKINGS BY STATUS (STAFF PANEL)
    // =========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getBookingsByStatus(
            @PathVariable String status) {

        return ResponseEntity.ok(
                bookingService.getBookingsByStatus(status)
        );
    }


    // =========================================
    // GET SCHEDULED MAINTENANCE
    // =========================================

    @GetMapping("/maintenance/scheduled")
    public ResponseEntity<?> getScheduledMaintenance() {

        return ResponseEntity.ok(
                maintenanceService.getPendingMaintenance()
                
        );
    }


    // =========================================
    // COMPLETE MAINTENANCE
    // =========================================

    @PostMapping("/maintenance/complete/{id}")
    public ResponseEntity<?> completeMaintenance(
            @PathVariable String id) {

        return ResponseEntity.ok(
                maintenanceService.completeMaintenance(id, "STAFF")
                
        );
    }


    // =========================================
    // PENALTY CALCULATOR
    // =========================================

    private double calculatePenalty(String severity) {

        if (severity == null) return 0;

        switch (severity.toUpperCase()) {

            case "LOW":
                return 2000;

            case "MEDIUM":
                return 6000;

            case "HIGH":
                return 15000;

            default:
                return 0;
        }
    }
}