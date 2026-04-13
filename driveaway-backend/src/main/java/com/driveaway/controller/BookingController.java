package com.driveaway.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.driveaway.dto.BookingRequest;
import com.driveaway.dto.InspectionRequest;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Damage;
import com.driveaway.entity.MaintenanceRecord;
import com.driveaway.service.BookingService;
import com.driveaway.service.DamageService;
import com.driveaway.service.MaintenanceService;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;
    private final DamageService damageService;
    private final MaintenanceService maintenanceService;

    @Autowired
    public BookingController(BookingService bookingService, DamageService damageService, MaintenanceService maintenanceService) {
        this.bookingService = bookingService;
        this.damageService = damageService;
        this.maintenanceService = maintenanceService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request, @RequestParam(defaultValue = "false") boolean licenseVerified) {
        try {
            Booking saved = bookingService.createBooking(request, licenseVerified);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return bookingService.getAllBookings().stream()
                .filter(b -> b.getId().equals(id)).findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(bookingService.getByUser(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Booking>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(bookingService.getByStatus(status));
    }

    @PutMapping("/{id}/handover")
    public ResponseEntity<?> handover(@PathVariable String id) {
        try {
            return ResponseEntity.ok(bookingService.handoverVehicle(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id) {
        try {
            return ResponseEntity.ok(bookingService.cancelBooking(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/return")
    public ResponseEntity<String> returnAndInspect(@RequestBody InspectionRequest request) {
        try {
            bookingService.returnVehicle(request.getBookingId(), request.isHasDamage());
            Damage damage = damageService.logDamage(request);
            if (damage != null) {
                maintenanceService.scheduleMaintenance(request.getVehicleId(), "Damage: " + request.getDamageDescription());
                return ResponseEntity.ok("Vehicle returned. Damage: " + damage.getSeverity() + ". Penalty: Rs." + damage.getPenaltyAmount() + ". Maintenance scheduled.");
            }
            return ResponseEntity.ok("Vehicle returned. No damage found.");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/maintenance/all")
    public ResponseEntity<List<MaintenanceRecord>> getAllMaintenance() {
        return ResponseEntity.ok(maintenanceService.getAll());
    }

    @GetMapping("/maintenance/scheduled")
    public ResponseEntity<List<MaintenanceRecord>> getScheduledMaintenance() {
        return ResponseEntity.ok(maintenanceService.getScheduled());
    }

    @PutMapping("/maintenance/{id}/complete")
    public ResponseEntity<?> completeMaintenance(@PathVariable String id) {
        try {
            return ResponseEntity.ok(maintenanceService.completeMaintenance(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/damages/{bookingId}")
    public ResponseEntity<List<Damage>> getDamagesByBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(damageService.getDamagesByBooking(bookingId));
    }
}
