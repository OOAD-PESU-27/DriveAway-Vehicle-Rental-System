package com.driveaway.controller;

import com.driveaway.dto.InspectionRequest;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Damage;
import com.driveaway.entity.MaintenanceRecord;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.DamageRepository;
import com.driveaway.repository.MaintenanceRepository;
import com.driveaway.repository.VehicleRepository;
import com.driveaway.entity.Vehicle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Person3BridgeController
 *
 * WHY THIS FILE EXISTS:
 *   The integrated backend uses /api/v1/bookings/** endpoints.
 *   Person 3's JavaFX frontend (StaffController + BookingController) calls:
 *     GET  /api/bookings                    → all bookings
 *     GET  /api/bookings/user/{userId}       → bookings by user
 *     GET  /api/bookings/status/{status}     → bookings by status (HANDED_OVER etc.)
 *     PUT  /api/bookings/{id}/handover       → mark CONFIRMED → HANDED_OVER
 *     POST /api/bookings/return              → return + damage inspection
 *     GET  /api/bookings/maintenance/scheduled → scheduled maintenance list
 *     PUT  /api/bookings/maintenance/{id}/complete → mark maintenance COMPLETED
 *
 *   These routes DID NOT EXIST in the integrated backend, causing:
 *     - "Request method 'PUT' is not supported"
 *     - "Internal Server Error" on /api/bookings/return
 *
 * HOW IT WORKS:
 *   This controller adds all those missing routes, wired directly to the
 *   existing Repositories. It does NOT change any existing controllers.
 *
 * WHERE TO PUT THIS FILE:
 *   driveaway-backend/src/main/java/com/driveaway/controller/Person3BridgeController.java
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class Person3BridgeController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DamageRepository damageRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    // ── GET ALL BOOKINGS ──────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingRepository.findAll());
    }

    // ── GET BY USER ───────────────────────────────────────────────────────────
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(bookingRepository.findByUserId(userId));
    }

    // ── GET BY STATUS (e.g. HANDED_OVER, CONFIRMED) ───────────────────────────
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Booking>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(bookingRepository.findByStatus(status.toUpperCase()));
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return bookingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── HANDOVER: CONFIRMED → HANDED_OVER ────────────────────────────────────
    /**
     * Called when customer clicks "Request Handover" in BookingController.
     * Frontend sends: PUT /api/bookings/{id}/handover
     * This was causing "PUT not supported" — now fixed.
     */
    @PutMapping("/{id}/handover")
    public ResponseEntity<?> handover(@PathVariable String id) {
        return bookingRepository.findById(id)
                .map(booking -> {
                    if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                        return ResponseEntity.badRequest()
                                .body("Can only hand over a CONFIRMED booking. Current: " + booking.getStatus());
                    }
                    booking.setStatus("HANDED_OVER");
                    bookingRepository.save(booking);

                    // Mark vehicle as booked/unavailable during handover period
                    vehicleRepository.findById(booking.getVehicleId()).ifPresent(v -> {
                        v.setAvailable(false);
                        v.setStatus("BOOKED");
                        vehicleRepository.save(v);
                    });

                    return ResponseEntity.ok((Object) booking);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── CANCEL ────────────────────────────────────────────────────────────────
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id) {
        return bookingRepository.findById(id)
                .map(booking -> {
                    if ("HANDED_OVER".equalsIgnoreCase(booking.getStatus())) {
                        return ResponseEntity.badRequest()
                                .body("Cannot cancel a handed-over booking.");
                    }
                    booking.setStatus("CANCELLED");
                    bookingRepository.save(booking);

                    // Restore vehicle availability
                    vehicleRepository.findById(booking.getVehicleId()).ifPresent(v -> {
                        v.setAvailable(true);
                        v.setStatus("AVAILABLE");
                        vehicleRepository.save(v);
                    });

                    return ResponseEntity.ok((Object) booking);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── RETURN + INSPECTION ───────────────────────────────────────────────────
    /**
     * Called when staff clicks "Submit Return Inspection" in StaffController.
     * Frontend sends: POST /api/bookings/return
     * Body: { bookingId, vehicleId, hasDamage, damageDescription, damageSeverity }
     *
     * Was causing "Internal Server Error" because the endpoint didn't exist.
     *
     * WHAT IT DOES:
     *   1. Marks booking as RETURNED
     *   2. If hasDamage=true → logs a Damage record + schedules Maintenance
     *   3. If hasDamage=false → restores vehicle to AVAILABLE
     */
    @PostMapping("/return")
    public ResponseEntity<String> returnAndInspect(@RequestBody InspectionRequest request) {
        try {
            // Step 1: Find and validate booking
            Booking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new RuntimeException(
                            "Booking not found: " + request.getBookingId()));

            if (!"HANDED_OVER".equalsIgnoreCase(booking.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Can only return a HANDED_OVER booking. Current status: " + booking.getStatus());
            }

            // Step 2: Mark booking as RETURNED
            booking.setStatus("RETURNED");
            bookingRepository.save(booking);

            // Step 3: Handle damage
            if (request.isHasDamage()) {
                // Log damage record
                Damage damage = new Damage();
                damage.setBookingId(request.getBookingId());
                damage.setVehicleId(request.getVehicleId());
                damage.setDescription(request.getDamageDescription());
                damage.setSeverity(
                        request.getDamageSeverity() != null
                        ? request.getDamageSeverity().toUpperCase()
                        : "LOW");
                damage.setEstimatedRepairCost(getPenalty(request.getDamageSeverity()));
                damage.setStatus("REPORTED");
                damage.setReportedBy("STAFF");
                damage.setReportedAt(LocalDateTime.now());
                damageRepository.save(damage);

                // Schedule maintenance
                MaintenanceRecord maint = new MaintenanceRecord();
                maint.setVehicleId(request.getVehicleId());
                maint.setDescription("Damage repair: " + request.getDamageDescription());
                maint.setMaintenanceType("REPAIR");
                maint.setStatus("SCHEDULED");
                maint.setScheduledDate(LocalDate.now().plusDays(1));
                maint.setCreatedAt(LocalDateTime.now());
                maint.setPerformedBy("SYSTEM");
                maint.setCost(getPenalty(request.getDamageSeverity()));
                maintenanceRepository.save(maint);

                // Mark vehicle under maintenance
                vehicleRepository.findById(request.getVehicleId()).ifPresent(v -> {
                    v.setAvailable(false);
                    v.setStatus("MAINTENANCE");
                    vehicleRepository.save(v);
                });

                String severity = damage.getSeverity();
                double penalty = damage.getEstimatedRepairCost();
                return ResponseEntity.ok(
                        "Vehicle returned. Damage: " + severity
                        + ". Penalty: Rs." + (int) penalty
                        + ". Maintenance scheduled.");

            } else {
                // No damage — restore vehicle to AVAILABLE
                vehicleRepository.findById(request.getVehicleId()).ifPresent(v -> {
                    v.setAvailable(true);
                    v.setStatus("AVAILABLE");
                    vehicleRepository.save(v);
                });
                return ResponseEntity.ok("Vehicle returned. No damage found.");
            }

        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ── MAINTENANCE ENDPOINTS ─────────────────────────────────────────────────

    /** Get all scheduled maintenance records */
    @GetMapping("/maintenance/scheduled")
    public ResponseEntity<List<MaintenanceRecord>> getScheduledMaintenance() {
        return ResponseEntity.ok(maintenanceRepository.findByStatus("SCHEDULED"));
    }

    /** Get all maintenance records */
    @GetMapping("/maintenance/all")
    public ResponseEntity<List<MaintenanceRecord>> getAllMaintenance() {
        return ResponseEntity.ok(maintenanceRepository.findAll());
    }

    /**
     * Mark a maintenance record as COMPLETED.
     * Frontend sends: PUT /api/bookings/maintenance/{id}/complete
     * Was causing "PUT not supported" — now fixed.
     */
    @PutMapping("/maintenance/{id}/complete")
    public ResponseEntity<?> completeMaintenance(@PathVariable String id) {
        return maintenanceRepository.findById(id)
                .map(record -> {
                    record.setStatus("COMPLETED");
                    record.setCompletedDate(LocalDate.now());
                    maintenanceRepository.save(record);

                    // Restore vehicle to AVAILABLE after maintenance done
                    vehicleRepository.findById(record.getVehicleId()).ifPresent(v -> {
                        v.setAvailable(true);
                        v.setStatus("AVAILABLE");
                        vehicleRepository.save(v);
                    });

                    return ResponseEntity.ok((Object) record);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Get damages by booking ────────────────────────────────────────────────
    @GetMapping("/damages/{bookingId}")
    public ResponseEntity<List<Damage>> getDamagesByBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(damageRepository.findByBookingId(bookingId));
    }

    // ── Penalty calculator ────────────────────────────────────────────────────
    private double getPenalty(String severity) {
        if (severity == null) return 0;
        switch (severity.toUpperCase()) {
            case "LOW":    return 2000.0;
            case "MEDIUM": return 6000.0;
            case "HIGH":   return 15000.0;
            default:       return 0.0;
        }
    }
}