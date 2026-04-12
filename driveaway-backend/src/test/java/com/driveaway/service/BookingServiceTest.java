package com.driveaway.service;

import com.driveaway.dto.BookingRequest;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Payment;
import com.driveaway.entity.Vehicle;
import com.driveaway.PaymentStatus;
import com.driveaway.exception.PaymentException;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
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
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private NotificationService notificationService;

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

    // ── cancelBooking – refund policy ─────────────────────────────────────────

    @Test
    void cancelBooking_moreThan7DaysBeforePickup_fullRefund() {
        Booking booking = buildBookingWithStartDate("bk-r10", "ACTIVE", LocalDate.now().plusDays(10));

        Payment payment = new Payment("bk-r10", "user-1", 6000.0, "CARD");
        payment.setId("pay-r10");
        payment.setStatus(PaymentStatus.COMPLETED);

        when(bookingRepository.findById("bk-r10")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());
        when(paymentRepository.findByRentalId("bk-r10")).thenReturn(List.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBooking("bk-r10", "user-1");

        assertEquals("CANCELLED", result.getStatus());
        // Full refund: payment status set to REFUNDED
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
        verify(notificationService).sendCancellationNotification(any(), eq(6000.0), eq("FULL_REFUND"));
    }

    @Test
    void cancelBooking_between2And7DaysBeforePickup_halfRefund() {
        Booking booking = buildBookingWithStartDate("bk-r11", "ACTIVE", LocalDate.now().plusDays(4));

        Payment payment = new Payment("bk-r11", "user-1", 4000.0, "CARD");
        payment.setId("pay-r11");
        payment.setStatus(PaymentStatus.COMPLETED);

        when(bookingRepository.findById("bk-r11")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());
        when(paymentRepository.findByRentalId("bk-r11")).thenReturn(List.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBooking("bk-r11", "user-1");

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
        verify(notificationService).sendCancellationNotification(any(), eq(2000.0), eq("PARTIAL_REFUND_50"));
    }

    @Test
    void cancelBooking_lessThan2DaysBeforePickup_noRefund() {
        Booking booking = buildBookingWithStartDate("bk-r12", "ACTIVE", LocalDate.now().plusDays(1));

        Payment payment = new Payment("bk-r12", "user-1", 3000.0, "CARD");
        payment.setId("pay-r12");
        payment.setStatus(PaymentStatus.COMPLETED);

        when(bookingRepository.findById("bk-r12")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());
        when(paymentRepository.findByRentalId("bk-r12")).thenReturn(List.of(payment));

        bookingService.cancelBooking("bk-r12", "user-1");

        // No refund: payment status must NOT change to REFUNDED
        verify(paymentRepository, never()).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
        verify(notificationService).sendCancellationNotification(any(), eq(0.0), eq("NO_REFUND"));
    }

    @Test
    void cancelBooking_noPaymentFound_noRefundAttempted() {
        Booking booking = buildBookingWithStartDate("bk-r13", "CONFIRMED", LocalDate.now().plusDays(10));

        when(bookingRepository.findById("bk-r13")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());
        when(paymentRepository.findByRentalId("bk-r13")).thenReturn(List.of());

        bookingService.cancelBooking("bk-r13", "user-1");

        // No eligible payment → no refund payment save
        verify(paymentRepository, never()).save(any());
        verify(notificationService).sendCancellationNotification(any(), eq(0.0), eq("NO_REFUND"));
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
        verify(notificationService).sendBookingConfirmedNotification(any(Booking.class));
    }

    // ── processReturn ────────────────────────────────────────────────────────

    @Test
    void processReturn_confirmedBooking_setsReturnedStatus() {
        Booking booking = buildBooking("bk-r01", "CONFIRMED");

        when(bookingRepository.findById("bk-r01")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());
        when(paymentRepository.findByRentalId("bk-r01")).thenReturn(List.of());

        Booking result = bookingService.processReturn("bk-r01", "Minor scratch", 200.0, "staff-1");

        assertEquals("RETURNED", result.getStatus());
        assertEquals("Minor scratch", result.getDamageNotes());
        assertEquals(200.0, result.getDamageCharge(), 0.001);
        assertNotNull(result.getReturnDate());
        verify(notificationService).sendVehicleReturnCompletedNotification(any(Booking.class));
        verify(notificationService).sendDamagePenaltyAppliedNotification(any(Booking.class));
    }

    @Test
    void processReturn_withSecurityDeposit_noDamage_refundsFullDeposit() {
        Booking booking = buildBooking("bk-r02", "ACTIVE");

        Payment payment = new Payment("bk-r02", "user-1", 5000.0, "CARD");
        payment.setId("pay-r02");
        payment.setSecurityDeposit(1000.0);
        payment.setSecurityDepositStatus("HELD");

        when(bookingRepository.findById("bk-r02")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());
        when(paymentRepository.findByRentalId("bk-r02")).thenReturn(List.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.processReturn("bk-r02", "", 0.0, "staff-1");

        assertEquals("RETURNED", result.getStatus());
        assertEquals(0.0, result.getDamageCharge(), 0.001);
        verify(paymentRepository).save(argThat(p -> "REFUNDED".equals(p.getSecurityDepositStatus())));
        verify(notificationService).sendVehicleReturnCompletedNotification(any(Booking.class));
        verify(notificationService, never()).sendDamagePenaltyAppliedNotification(any(Booking.class));
    }

    @Test
    void processReturn_withSecurityDeposit_damageExceedsDeposit_forfeit() {
        Booking booking = buildBooking("bk-r03", "ACTIVE");

        Payment payment = new Payment("bk-r03", "user-1", 5000.0, "CARD");
        payment.setId("pay-r03");
        payment.setSecurityDeposit(500.0);
        payment.setSecurityDepositStatus("HELD");

        when(bookingRepository.findById("bk-r03")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(vehicleService).markVehicleAsAvailable(any());
        when(paymentRepository.findByRentalId("bk-r03")).thenReturn(List.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.processReturn("bk-r03", "Major damage", 800.0, "staff-1");

        assertEquals("RETURNED", result.getStatus());
        verify(paymentRepository).save(argThat(p -> "FORFEITED".equals(p.getSecurityDepositStatus())));
    }

    @Test
    void processReturn_alreadyReturnedBooking_throwsPaymentException() {
        Booking booking = buildBooking("bk-r04", "RETURNED");

        when(bookingRepository.findById("bk-r04")).thenReturn(Optional.of(booking));

        assertThrows(PaymentException.class,
                () -> bookingService.processReturn("bk-r04", "", 0.0, "staff-1"));
    }

    @Test
    void processReturn_cancelledBooking_throwsPaymentException() {
        Booking booking = buildBooking("bk-r05", "CANCELLED");

        when(bookingRepository.findById("bk-r05")).thenReturn(Optional.of(booking));

        assertThrows(PaymentException.class,
                () -> bookingService.processReturn("bk-r05", "", 0.0, "staff-1"));
    }

    // ── booking status filtering ──────────────────────────────────────────────

    @Test
    void getActiveBookings_returnsConfirmedAndActiveStatuses() {
        Booking confirmed = buildBooking("bk-a01", "CONFIRMED");
        Booking active    = buildBooking("bk-a02", "ACTIVE");

        when(bookingRepository.findByStatus("CONFIRMED")).thenReturn(List.of(confirmed));
        when(bookingRepository.findByStatus("ACTIVE")).thenReturn(List.of(active));

        List<Booking> result = bookingService.getActiveBookings();

        assertEquals(2, result.size(), "Active bookings should include CONFIRMED and ACTIVE");
        assertTrue(result.stream().anyMatch(b -> "CONFIRMED".equals(b.getStatus())));
        assertTrue(result.stream().anyMatch(b -> "ACTIVE".equals(b.getStatus())));
    }

    @Test
    void getActiveBookings_whenNoConfirmedOrActive_returnsEmptyList() {
        when(bookingRepository.findByStatus("CONFIRMED")).thenReturn(List.of());
        when(bookingRepository.findByStatus("ACTIVE")).thenReturn(List.of());

        List<Booking> result = bookingService.getActiveBookings();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCompletedBookings_returnsOnlyCompletedStatus() {
        Booking completed = buildBooking("bk-c01", "COMPLETED");

        when(bookingRepository.findByStatus("COMPLETED")).thenReturn(List.of(completed));

        List<Booking> result = bookingService.getCompletedBookings();

        assertEquals(1, result.size());
        assertEquals("COMPLETED", result.get(0).getStatus());
    }

    @Test
    void getCompletedBookings_whenNone_returnsEmptyList() {
        when(bookingRepository.findByStatus("COMPLETED")).thenReturn(List.of());

        List<Booking> result = bookingService.getCompletedBookings();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllBookings_delegatesToRepository() {
        Booking b1 = buildBooking("bk-all01", "CONFIRMED");
        Booking b2 = buildBooking("bk-all02", "COMPLETED");
        Booking b3 = buildBooking("bk-all03", "CANCELLED");

        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        List<Booking> result = bookingService.getAllBookings();

        assertEquals(3, result.size(), "getAllBookings must return all bookings regardless of status");
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

    private Booking buildBookingWithStartDate(String id, String status, LocalDate startDate) {
        Booking b = new Booking();
        b.setId(id);
        b.setUserId("user-1");
        b.setVehicleId("v-1");
        b.setStartDate(startDate);
        b.setEndDate(startDate.plusDays(3));
        b.setTotalPrice(5000.0);
        b.setStatus(status);
        return b;
    }
}
