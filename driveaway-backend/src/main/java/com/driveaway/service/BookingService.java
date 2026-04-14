package com.driveaway.service;

import com.driveaway.dto.BookingRequest;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Payment;
import com.driveaway.entity.Vehicle;
import com.driveaway.PaymentStatus;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.HolidayRepository;
import com.driveaway.exception.ResourceNotFoundException;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import com.driveaway.service.pricing.PricingService;

/**
 * BookingService - Contains business logic for vehicle bookings
 * GRASP: Information Expert - Handles booking-related business logic
 * SOLID: SRP - Only handles booking operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleService vehicleService;
    private final PricingService pricingService;
    private final AuditLogService auditLogService;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final HolidayRepository holidayRepository;

    /**
     * Create a new booking
     *
     * FIX: Removed the vehicle.isAvailable() flag check.
     * That flag is a persistent DB flag that stays false after any booking,
     * causing ALL future booking attempts on that vehicle to fail with
     * "Vehicle is not available". The correct availability check is the
     * date-range overlap query against confirmed bookings (below), which
     * correctly allows re-booking after cancellation or completion.
     */
    public Booking createBooking(BookingRequest request, String userId) {
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new PaymentException("End date must be after start date");
        }

        // Verify vehicle exists
        Vehicle vehicle = vehicleService.getVehicleById(request.getVehicleId());

        // FIX: Only use date-range overlap check — NOT vehicle.isAvailable().
        // The isAvailable() flag is unreliable: it stays false permanently after
        // the first booking and blocks all subsequent bookings on the same vehicle.
        boolean overlapping = bookingRepository
                .existsByVehicleIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        request.getVehicleId(), "CONFIRMED",
                        request.getEndDate(), request.getStartDate());
        if (overlapping) {
            throw new PaymentException("Vehicle is already booked for the selected period");
        }

        // Build the date list between start and end (inclusive)
        List<LocalDate> rentalDays = getDatesBetween(request.getStartDate(), request.getEndDate());

        // Get holidays as list of strings
        List<String> holidays = holidayRepository.findAll()
                .stream()
                .map(h -> h.getDate())
                .toList();

        double basePrice = vehicle.getPricePerDay();
        double totalPrice = pricingService.calculateTotalPrice(basePrice, rentalDays, holidays);

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setVehicleId(request.getVehicleId());
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setTotalPrice(totalPrice);
        booking.setStatus("CONFIRMED");

        Booking savedBooking = bookingRepository.save(booking);

        vehicleService.markVehicleAsBooked(request.getVehicleId());
        notificationService.sendBookingConfirmedNotification(savedBooking);

        auditLogService.logPaymentAction("BOOKING_CREATED", savedBooking.getId(), userId,
                "Booking created for vehicle: " + request.getVehicleId()
                        + " from " + request.getStartDate() + " to " + request.getEndDate());

        return savedBooking;
    }

    private List<LocalDate> getDatesBetween(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }

    /**
     * Get booking by ID
     */
    public Booking getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    /**
     * Get all bookings for a user
     */
    public List<Booking> getBookingsByUserId(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    /**
     * Get all bookings (admin)
     */
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * Get all active bookings (status CONFIRMED or ACTIVE)
     */
    public List<Booking> getActiveBookings() {
        List<Booking> confirmed = bookingRepository.findByStatus("CONFIRMED");
        List<Booking> active = bookingRepository.findByStatus("ACTIVE");
        List<Booking> result = new java.util.ArrayList<>(confirmed);
        result.addAll(active);
        return result;
    }

    /**
     * Get all completed bookings
     */
    public List<Booking> getCompletedBookings() {
        return bookingRepository.findByStatus("COMPLETED");
    }

    /**
     * Cancel a booking with partial-refund policy.
     *
     * Refund tiers (based on days remaining until pickup):
     *  > 7 days  → FULL refund (100 %)
     *  2–7 days  → PARTIAL refund (50 %)
     *  < 2 days  → NO refund (0 %)
     *
     * A refund is only issued when an associated completed/successful payment is found.
     */
    public Booking cancelBooking(String bookingId, String userId) {
        Booking booking = getBookingById(bookingId);

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new PaymentException("Booking is already cancelled");
        }
        if ("COMPLETED".equals(booking.getStatus())) {
            throw new PaymentException("Completed bookings cannot be cancelled");
        }

        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);

        vehicleService.markVehicleAsAvailable(booking.getVehicleId());

        auditLogService.logPaymentAction("BOOKING_CANCELLED", bookingId, userId,
                "Booking cancelled: " + bookingId);

        // ── Cancellation refund policy ────────────────────────────────────
        double refundAmount = 0.0;
        String refundPolicy = "NO_REFUND";
        try {
            long daysUntilPickup = booking.getStartDate() != null
                    ? ChronoUnit.DAYS.between(LocalDate.now(), booking.getStartDate())
                    : -1;

            List<Payment> payments = paymentRepository.findByRentalId(bookingId);
            Payment eligiblePayment = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.COMPLETED
                              || p.getStatus() == PaymentStatus.SUCCESS)
                    .findFirst()
                    .orElse(null);

            if (eligiblePayment != null && daysUntilPickup >= 0) {
                double paidAmount = eligiblePayment.getAmount();
                if (daysUntilPickup > 7) {
                    refundAmount = paidAmount;          // 100 %
                    refundPolicy = "FULL_REFUND";
                } else if (daysUntilPickup >= 2) {
                    refundAmount = paidAmount * 0.5;    // 50 %
                    refundPolicy = "PARTIAL_REFUND_50";
                } else {
                    refundAmount = 0.0;                 // 0 %
                    refundPolicy = "NO_REFUND";
                }

                if (refundAmount > 0) {
                    eligiblePayment.setStatus(PaymentStatus.REFUNDED);
                    eligiblePayment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(eligiblePayment);
                    log.info("[BOOKING] Refund {} applied for booking={} policy={} daysLeft={}",
                            refundAmount, bookingId, refundPolicy, daysUntilPickup);
                }
            }
        } catch (Exception e) {
            log.warn("[BOOKING] Could not process cancellation refund for booking={}: {}",
                    bookingId, e.getMessage(), e);
        }

        notificationService.sendCancellationNotification(saved, refundAmount, refundPolicy);

        return saved;
    }

    /**
     * Complete a booking (vehicle returned)
     */
    public Booking completeBooking(String bookingId, String staffId) {
        Booking booking = getBookingById(bookingId);

        if (!"CONFIRMED".equals(booking.getStatus()) && !"ACTIVE".equals(booking.getStatus())) {
            throw new PaymentException("Only confirmed or active (paid) bookings can be completed");
        }

        booking.setStatus("COMPLETED");
        Booking saved = bookingRepository.save(booking);

        vehicleService.markVehicleAsAvailable(booking.getVehicleId());

        auditLogService.logPaymentAction("BOOKING_COMPLETED", bookingId, staffId,
                "Booking completed: " + bookingId);

        return saved;
    }

    /**
     * Process vehicle return with damage check.
     */
    public Booking processReturn(String bookingId, String damageNotes, double damageCharge, String staffId) {
        Booking booking = getBookingById(bookingId);

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new PaymentException("Cancelled bookings cannot be returned");
        }
        if ("RETURNED".equals(booking.getStatus())) {
            throw new PaymentException("Booking has already been returned");
        }

        booking.setStatus("RETURNED");
        booking.setReturnDate(LocalDateTime.now());
        booking.setDamageNotes(damageNotes != null ? damageNotes : "");
        booking.setDamageCharge(Math.max(0, damageCharge));

        Booking saved = bookingRepository.save(booking);

        vehicleService.markVehicleAsAvailable(booking.getVehicleId());

        boolean depositProcessed = false;
        try {
            List<com.driveaway.entity.Payment> payments = paymentRepository.findByRentalId(bookingId);
            for (com.driveaway.entity.Payment payment : payments) {
                if (payment.getSecurityDeposit() > 0 && "HELD".equals(payment.getSecurityDepositStatus())) {
                    double deposit = payment.getSecurityDeposit();
                    double refund = Math.max(0, deposit - damageCharge);
                    if (refund <= 0) {
                        payment.setSecurityDepositStatus("FORFEITED");
                    } else {
                        payment.setSecurityDepositStatus("REFUNDED");
                    }
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    depositProcessed = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("[BOOKING] Could not process security deposit for booking={}: {}", bookingId, e.getMessage(), e);
        }

        saved.setDepositRefunded(depositProcessed);
        bookingRepository.save(saved);

        auditLogService.logPaymentAction("BOOKING_RETURNED", bookingId, staffId,
                "Vehicle returned. Damage: " + (damageCharge > 0 ? "₹" + damageCharge : "None"));

        notificationService.sendVehicleReturnCompletedNotification(saved);
        if (saved.getDamageCharge() > 0) {
            notificationService.sendDamagePenaltyAppliedNotification(saved);
        }

        return saved;
    }

    /**
     * Get bookings for admin with status filtering and simple search support.
     */
    public List<Booking> getBookingsForAdmin(String status, String search) {
        List<Booking> result;
        if ("active".equalsIgnoreCase(status)) {
            result = getActiveBookings();
        } else if ("completed".equalsIgnoreCase(status)) {
            result = getCompletedBookings();
        } else {
            result = getAllBookings();
        }

        if (search == null || search.isBlank()) {
            return result;
        }

        String q = search.trim().toLowerCase(Locale.ROOT);
        return result.stream().filter(b ->
                        containsIgnoreCase(b.getId(), q) ||
                        containsIgnoreCase(b.getUserId(), q) ||
                        containsIgnoreCase(b.getVehicleId(), q) ||
                        containsIgnoreCase(b.getStatus(), q))
                .toList();
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
