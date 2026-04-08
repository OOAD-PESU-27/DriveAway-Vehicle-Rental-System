package com.driveaway.service;

import com.driveaway.dto.BookingRequest;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Vehicle;
import com.driveaway.exception.PaymentException;
import com.driveaway.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService – covers booking lifecycle state transitions.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private VehicleService vehicleService;
    @Mock
    private PricingService pricingService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BookingService bookingService;

    // ── completeBooking – CONFIRMED status ───────────────────────────────────

    @Test
    void completeBooking_confirmedStatus_setsCompleted() {
        Booking booking = buildBooking("bk-001", "CONFIRMED");

        when(bookingRepository.findById("bk-001")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());

        Booking result = bookingService.completeBooking("bk-001", "staff-1");

        assertEquals("COMPLETED", result.getStatus());
        verify(bookingRepository).save(argThat(b -> "COMPLETED".equals(b.getStatus())));
    }

    @Test
    void completeBooking_activeStatus_setsCompleted() {
        // ACTIVE = CONFIRMED + payment made; staff should still be able to complete it.
        Booking booking = buildBooking("bk-002", "ACTIVE");

        when(bookingRepository.findById("bk-002")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());

        Booking result = bookingService.completeBooking("bk-002", "staff-1");

        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void completeBooking_alreadyCompletedStatus_throwsPaymentException() {
        Booking booking = buildBooking("bk-003", "COMPLETED");

        when(bookingRepository.findById("bk-003")).thenReturn(Optional.of(booking));

        assertThrows(PaymentException.class,
                () -> bookingService.completeBooking("bk-003", "staff-1"));
    }

    @Test
    void completeBooking_cancelledStatus_throwsPaymentException() {
        Booking booking = buildBooking("bk-004", "CANCELLED");

        when(bookingRepository.findById("bk-004")).thenReturn(Optional.of(booking));

        assertThrows(PaymentException.class,
                () -> bookingService.completeBooking("bk-004", "staff-1"));
    }

    // ── cancelBooking ────────────────────────────────────────────────────────

    @Test
    void cancelBooking_confirmedStatus_setsCancelled() {
        Booking booking = buildBooking("bk-010", "CONFIRMED");

        when(bookingRepository.findById("bk-010")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());

        Booking result = bookingService.cancelBooking("bk-010", "user-1");

        assertEquals("CANCELLED", result.getStatus());
    }

    @Test
    void cancelBooking_activeStatus_setsCancelled() {
        // ACTIVE (paid) bookings can still be cancelled (refund handled separately)
        Booking booking = buildBooking("bk-011", "ACTIVE");

        when(bookingRepository.findById("bk-011")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());

        Booking result = bookingService.cancelBooking("bk-011", "user-1");

        assertEquals("CANCELLED", result.getStatus());
    }

    @Test
    void cancelBooking_alreadyCancelled_throwsPaymentException() {
        Booking booking = buildBooking("bk-012", "CANCELLED");

        when(bookingRepository.findById("bk-012")).thenReturn(Optional.of(booking));

        assertThrows(PaymentException.class,
                () -> bookingService.cancelBooking("bk-012", "user-1"));
    }

    @Test
    void cancelBooking_completed_throwsPaymentException() {
        Booking booking = buildBooking("bk-013", "COMPLETED");

        when(bookingRepository.findById("bk-013")).thenReturn(Optional.of(booking));

        assertThrows(PaymentException.class,
                () -> bookingService.cancelBooking("bk-013", "user-1"));
    }

    // ── createBooking ────────────────────────────────────────────────────────

    @Test
    void createBooking_setsConfirmedStatus() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId("v-1");
        vehicle.setAvailable(true);

        when(vehicleService.getVehicleById("v-1")).thenReturn(vehicle);
        when(bookingRepository.existsByVehicleIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                any(), any(), any(), any())).thenReturn(false);
        when(pricingService.calculateTotalPrice(any(), anyLong())).thenReturn(5000.0);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId("bk-new");
            return b;
        });
        doNothing().when(vehicleService).markVehicleAsBooked(any());

        BookingRequest req = new BookingRequest();
        req.setVehicleId("v-1");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(3));

        Booking result = bookingService.createBooking(req, "user-1");

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(5000.0, result.getTotalPrice(), 0.001);
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private Booking buildBooking(String id, String status) {
        Booking b = new Booking();
        b.setId(id);
        b.setUserId("user-1");
        b.setVehicleId("v-1");
        b.setStartDate(LocalDate.now());
        b.setEndDate(LocalDate.now().plusDays(3));
        b.setTotalPrice(5000.0);
        b.setStatus(status);
        return b;
    }
}
