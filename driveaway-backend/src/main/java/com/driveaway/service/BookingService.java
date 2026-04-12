package com.driveaway.service;

import com.driveaway.dto.BookingRequest;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Vehicle;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.exception.ResourceNotFoundException;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * BookingService - Contains business logic for vehicle bookings
 * GRASP: Information Expert - Handles booking-related business logic
 * SOLID: SRP - Only handles booking operations
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleService vehicleService;
    private final PricingService pricingService;
    private final AuditLogService auditLogService;
    private final PaymentRepository paymentRepository;

    /**
     * Create a new booking
     */
    public Booking createBooking(BookingRequest request, String userId) {
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new PaymentException("End date must be after start date");
        }

        Vehicle vehicle = vehicleService.getVehicleById(request.getVehicleId());

        if (!vehicle.isAvailable()) {
            throw new PaymentException("Vehicle is not available for the selected dates");
        }

        boolean overlapping = bookingRepository
                .existsByVehicleIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        request.getVehicleId(), "CONFIRMED",
                        request.getEndDate(), request.getStartDate());
        if (overlapping) {
            throw new PaymentException("Vehicle is already booked for the selected period");
        }

        long rentalDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        double totalPrice = pricingService.calculateTotalPrice(request.getVehicleId(), rentalDays);

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setVehicleId(request.getVehicleId());
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setTotalPrice(totalPrice);
        booking.setStatus("CONFIRMED");

        Booking savedBooking = bookingRepository.save(booking);

        vehicleService.markVehicleAsBooked(request.getVehicleId());

        auditLogService.logPaymentAction("BOOKING_CREATED", savedBooking.getId(), userId,
                "Booking created for vehicle: " + request.getVehicleId()
                        + " from " + request.getStartDate() + " to " + request.getEndDate());

        return savedBooking;
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
     * Cancel a booking
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
     * Records the return, captures damage details, and triggers security deposit refund/forfeiture.
     * @param bookingId    the booking being returned
     * @param damageNotes  description of any damage (can be blank if no damage)
     * @param damageCharge charge for damage (0 = no damage, full deposit refunded)
     * @param staffId      staff member processing the return
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

        // Process security deposit refund/forfeiture based on damage check
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
            // Deposit processing failure should not block the return
        }

        saved.setDepositRefunded(depositProcessed);
        bookingRepository.save(saved);

        auditLogService.logPaymentAction("BOOKING_RETURNED", bookingId, staffId,
                "Vehicle returned. Damage: " + (damageCharge > 0 ? "₹" + damageCharge : "None"));

        return saved;
    }
}