package com.driveaway.service;

import com.driveaway.dto.BookingRequest;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Vehicle;
import com.driveaway.repository.BookingRepository;
import com.driveaway.exception.ResourceNotFoundException;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
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
}