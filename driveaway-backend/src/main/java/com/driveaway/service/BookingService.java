package com.driveaway.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.driveaway.dto.BookingRequest;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Vehicle;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.VehicleRepository;

@Service
public class BookingService {

    private static final double DEFAULT_DAILY_RATE = 1500.0;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository, VehicleRepository vehicleRepository) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public Booking createBooking(BookingRequest request, boolean licenseVerified) {
        if (!licenseVerified) {
            throw new RuntimeException("Booking failed: driving license not verified.");
        }

        LocalDate start = LocalDate.parse(request.getStartDate());
        LocalDate end = LocalDate.parse(request.getEndDate());

        if (!end.isAfter(start)) {
            throw new RuntimeException("End date must be after start date.");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found."));

        if (!vehicle.isAvailable()) {
            throw new RuntimeException("Vehicle is not available.");
        }

        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) {
            throw new RuntimeException("Booking duration must be at least one day.");
        }

        double pricePerDay = vehicle.getPricePerDay() > 0 ? vehicle.getPricePerDay() : DEFAULT_DAILY_RATE;
        double totalPrice = days * pricePerDay;

        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setVehicleId(request.getVehicleId());
        booking.setStartDate(start);
        booking.setEndDate(end);
        booking.setTotalPrice(totalPrice);
        booking.setStatus("CONFIRMED");
        booking.setLicenseVerified(true);
        booking.setCreatedAt(LocalDate.now());

        vehicle.setAvailable(false);
        vehicle.setStatus("BOOKED");
        vehicleRepository.save(vehicle);

        return bookingRepository.save(booking);
    }

    public Booking handoverVehicle(String bookingId) {
        Booking booking = findOrThrow(bookingId);
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Can only hand over confirmed booking.");
        }
        booking.setStatus("HANDED_OVER");
        return bookingRepository.save(booking);
    }

    public Booking returnVehicle(String bookingId, boolean hasDamage) {
        Booking booking = findOrThrow(bookingId);
        if (!"HANDED_OVER".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Can only return handed-over booking.");
        }
        booking.setStatus("RETURNED");
        Booking saved = bookingRepository.save(booking);

        if (!hasDamage) {
            restoreVehicle(booking.getVehicleId());
        }

        return saved;
    }

    public Booking cancelBooking(String bookingId) {
        Booking booking = findOrThrow(bookingId);
        if ("HANDED_OVER".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Cannot cancel handed-over booking.");
        }
        booking.setStatus("CANCELLED");
        Booking cancelled = bookingRepository.save(booking);
        restoreVehicle(booking.getVehicleId());
        return cancelled;
    }

    public List<Booking> getAllBookings() { return bookingRepository.findAll(); }
    public List<Booking> getByUser(String userId) { return bookingRepository.findByUserId(userId); }
    public List<Booking> getByStatus(String status) { return bookingRepository.findByStatus(status.toUpperCase()); }

    private Booking findOrThrow(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found."));
    }

    private void restoreVehicle(String vehicleId) {
        vehicleRepository.findById(vehicleId).ifPresent(vehicle -> {
            vehicle.setAvailable(true);
            vehicle.setStatus("AVAILABLE");
            vehicleRepository.save(vehicle);
        });
    }
}
