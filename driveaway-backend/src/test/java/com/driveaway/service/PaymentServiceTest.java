package com.driveaway.service;

import com.driveaway.PaymentStatus;
import com.driveaway.dto.PaymentRequest;
import com.driveaway.dto.PaymentResponse;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Payment;
import com.driveaway.entity.User;
import com.driveaway.exception.PaymentException;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService approval-gate workflow.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ReportService reportService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentEmailVerificationService emailVerificationService;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PaymentRequest();
        validRequest.setRentalId("rental-001");
        validRequest.setUserId("user-001");
        validRequest.setAmount(2500.0);
        validRequest.setPaymentMethod("CARD");
        validRequest.setCardNumber("4111111111111111");
    }

    // -------------------------------------------------------------------------
    // initiatePaymentRequest tests
    // -------------------------------------------------------------------------

    @Test
    void initiatePaymentRequest_createsPaymentInRequestedState() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("pay-001");
            return p;
        });

        PaymentResponse response = paymentService.initiatePaymentRequest(validRequest, "user-001");

        assertTrue(response.isSuccess());
        assertEquals(PaymentStatus.REQUESTED, response.getStatus());
        assertTrue(response.getMessage().contains("Awaiting approval"));
        verify(notificationService).sendPaymentRequestNotification(any(Payment.class), anyString());
        verify(auditLogService).logPaymentAction(eq("PAYMENT_REQUESTED"), any(), eq("user-001"), any());
    }

    @Test
    void initiatePaymentRequest_withSecurityDeposit_setsDepositHeld() {
        validRequest.setSecurityDeposit(500.0);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("pay-002");
            return p;
        });

        PaymentResponse response = paymentService.initiatePaymentRequest(validRequest, "user-001");

        assertTrue(response.isSuccess());
        verify(paymentRepository).save(argThat(p -> p.getSecurityDeposit() == 500.0
                && "HELD".equals(p.getSecurityDepositStatus())));
    }

    @Test
    void initiatePaymentRequest_withZeroAmount_throwsPaymentException() {
        validRequest.setAmount(0.0);
        assertThrows(PaymentException.class,
                () -> paymentService.initiatePaymentRequest(validRequest, "user-001"));
    }

    // -------------------------------------------------------------------------
    // approvePayment tests
    // -------------------------------------------------------------------------

    @Test
    void approvePayment_changesStatusToApproved() {
        Payment pending = new Payment("r1", "u1", 1000.0, "CARD");
        pending.setId("pay-003");
        pending.setStatus(PaymentStatus.REQUESTED);

        when(paymentRepository.findById("pay-003")).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.approvePayment("pay-003", "admin-001");

        assertTrue(response.isSuccess());
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        verify(notificationService).sendPaymentApprovalNotification(any(Payment.class));
        verify(auditLogService).logPaymentAction(eq("PAYMENT_APPROVED"), eq("pay-003"), eq("admin-001"), any());
    }

    @Test
    void approvePayment_whenAlreadyCompleted_throwsPaymentException() {
        Payment completed = new Payment("r1", "u1", 1000.0, "CARD");
        completed.setId("pay-004");
        completed.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findById("pay-004")).thenReturn(Optional.of(completed));

        assertThrows(PaymentException.class,
                () -> paymentService.approvePayment("pay-004", "admin-001"));
    }

    @Test
    void approvePayment_whenAlreadyApproved_returnsIdempotentSuccess() {
        // Simulates double-click or repeated approval: payment already APPROVED
        Payment alreadyApproved = new Payment("r1", "u1", 1000.0, "CARD");
        alreadyApproved.setId("pay-008");
        alreadyApproved.setStatus(PaymentStatus.APPROVED);
        alreadyApproved.setApprovedAt(LocalDateTime.now());
        alreadyApproved.setApprovedBy("admin-001");

        when(paymentRepository.findById("pay-008")).thenReturn(Optional.of(alreadyApproved));

        // Should NOT throw – must return a success response
        PaymentResponse response = assertDoesNotThrow(
                () -> paymentService.approvePayment("pay-008", "admin-001"),
                "approvePayment on an already-APPROVED payment must be idempotent");

        assertTrue(response.isSuccess(), "Idempotent approve must return success=true");
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
        // Repository save must NOT be called again (no state change needed)
        verify(paymentRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // completePayment tests
    // -------------------------------------------------------------------------

    @Test
    void completePayment_whenNotApproved_throwsPaymentException() {
        Payment requested = new Payment("r1", "u1", 1000.0, "CARD");
        requested.setId("pay-005");
        requested.setStatus(PaymentStatus.REQUESTED);

        when(paymentRepository.findById("pay-005")).thenReturn(Optional.of(requested));

        assertThrows(PaymentException.class,
                () -> paymentService.completePayment("pay-005", "user-001"));
    }

    @Test
    void completePayment_whenAlreadyCompleted_returnsIdempotentSuccess() {
        // Simulates double-click: payment already COMPLETED
        Payment alreadyCompleted = new Payment("r1", "u1", 10000.0, "CARD");
        alreadyCompleted.setId("pay-009");
        alreadyCompleted.setStatus(PaymentStatus.COMPLETED);
        alreadyCompleted.setTransactionId("TXN_EXISTING");
        alreadyCompleted.setPaymentDate(LocalDateTime.now().minusMinutes(5));

        when(paymentRepository.findById("pay-009")).thenReturn(Optional.of(alreadyCompleted));

        // Should NOT throw – must return a success response
        PaymentResponse response = assertDoesNotThrow(
                () -> paymentService.completePayment("pay-009", "user-001"),
                "completePayment on an already-COMPLETED payment must be idempotent");

        assertTrue(response.isSuccess(), "Idempotent complete must return success=true");
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertEquals(10000.0, response.getAmount(), "Amount must be preserved on idempotent complete");
        // Repository save must NOT be called again (no state change)
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePayment_afterApproval_persistsNonZeroAmount() {
        // Verifies the paid amount is not lost during completePayment
        Payment approved = new Payment("r1", "u1", 10000.0, "CARD");
        approved.setId("pay-010");
        approved.setStatus(PaymentStatus.APPROVED);
        approved.setApprovedAt(LocalDateTime.now());
        approved.setApprovedBy("admin-001");

        when(paymentRepository.findById("pay-010")).thenReturn(Optional.of(approved));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.completePayment("pay-010", "user-001");
        assertNotNull(response);
        // The exact amount entered must be preserved on both COMPLETED and FAILED outcomes
        assertEquals(10000.0, response.getAmount(),
                "Paid amount must equal the originally requested amount after completePayment");
    }

    @Test
    void completePayment_afterApproval_processesPayment() {
        Payment approved = new Payment("r1", "u1", 1000.0, "CARD");
        approved.setId("pay-006");
        approved.setStatus(PaymentStatus.APPROVED);
        approved.setApprovedAt(LocalDateTime.now());
        approved.setApprovedBy("admin-001");

        when(paymentRepository.findById("pay-006")).thenReturn(Optional.of(approved));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // completePayment has a random gateway; run it multiple times to cover both paths
        // We just verify it doesn't throw and returns a response
        PaymentResponse response = paymentService.completePayment("pay-006", "user-001");
        assertNotNull(response);
        // Status is either COMPLETED or FAILED (random gateway)
        assertTrue(response.getStatus() == PaymentStatus.COMPLETED ||
                   response.getStatus() == PaymentStatus.FAILED);
    }

    // -------------------------------------------------------------------------
    // approvePaymentByToken tests
    // -------------------------------------------------------------------------

    @Test
    void approvePaymentByToken_withInvalidToken_throwsPaymentException() {
        when(paymentRepository.findByApprovalToken("BAD_TOKEN")).thenReturn(Optional.empty());

        assertThrows(PaymentException.class,
                () -> paymentService.approvePaymentByToken("BAD_TOKEN"));
    }

    @Test
    void approvePaymentByToken_withValidToken_approvesPayment() {
        Payment requested = new Payment("r1", "u1", 1000.0, "CARD");
        requested.setId("pay-007");
        requested.setStatus(PaymentStatus.REQUESTED);
        requested.setApprovalToken("APPR_VALIDTOKEN");

        when(paymentRepository.findByApprovalToken("APPR_VALIDTOKEN")).thenReturn(Optional.of(requested));
        when(paymentRepository.findById("pay-007")).thenReturn(Optional.of(requested));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.approvePaymentByToken("APPR_VALIDTOKEN");
        assertTrue(response.isSuccess());
        assertEquals(PaymentStatus.APPROVED, response.getStatus());
    }

    // -------------------------------------------------------------------------
    // Email confirmation on payment success
    // -------------------------------------------------------------------------

    @Test
    void completePayment_emailFailure_doesNotAffectPaymentCompletion() {
        // Arrange: payment is in APPROVED state
        Payment approved = new Payment("r1", "u1", 1000.0, "CARD");
        approved.setId("pay-100");
        approved.setStatus(PaymentStatus.APPROVED);

        User user = new User();
        user.setId("u1");
        user.setEmail("john@example.com");
        user.setName("John Doe");

        when(paymentRepository.findById("pay-100")).thenReturn(Optional.of(approved));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        // Email service throws an exception – should be swallowed
        doThrow(new RuntimeException("SMTP error"))
                .when(emailVerificationService)
                .sendVerificationEmail(any(Payment.class), anyString(), anyString());

        // Act: completePayment should not throw regardless of email failure
        assertDoesNotThrow(() -> paymentService.completePayment("pay-100", "u1"),
                "Email failure must not propagate and roll back payment completion");
    }

    @Test
    void completePayment_whenUserHasEmail_triggersConfirmationEmail() {
        Payment approved = new Payment("r1", "u1", 1000.0, "CARD");
        approved.setId("pay-101");
        approved.setStatus(PaymentStatus.APPROVED);

        User user = new User();
        user.setId("u1");
        user.setEmail("john@example.com");
        user.setName("John Doe");

        when(paymentRepository.findById("pay-101")).thenReturn(Optional.of(approved));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        // Run up to 20 times to get at least one COMPLETED result (95% success rate)
        PaymentResponse response = null;
        for (int i = 0; i < 20; i++) {
            approved.setStatus(PaymentStatus.APPROVED); // reset for retry
            response = paymentService.completePayment("pay-101", "u1");
            if (response.getStatus() == PaymentStatus.COMPLETED) break;
        }

        assertNotNull(response);
        if (response.getStatus() == PaymentStatus.COMPLETED) {
            // Verify confirmation email was triggered with user's name and email
            verify(emailVerificationService, atLeastOnce())
                    .sendVerificationEmail(
                            any(Payment.class),
                            eq("john@example.com"),
                            eq("John Doe"));
        }
    }

    // -------------------------------------------------------------------------
    // paidAmount update on booking
    // -------------------------------------------------------------------------

    @Test
    void completePayment_afterApproval_updatesPaidAmountOnBooking() {
        Payment approved = new Payment("booking-abc", "u1", 10000.0, "CARD");
        approved.setId("pay-200");
        approved.setStatus(PaymentStatus.APPROVED);

        Booking booking = new Booking();
        booking.setId("booking-abc");
        booking.setTotalPrice(5000.0);

        when(paymentRepository.findById("pay-200")).thenReturn(Optional.of(approved));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.findById("booking-abc")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Run multiple times to get at least one COMPLETED result
        PaymentResponse response = null;
        for (int i = 0; i < 20; i++) {
            approved.setStatus(PaymentStatus.APPROVED);
            booking.setPaidAmount(0.0); // reset for retry
            response = paymentService.completePayment("pay-200", "u1");
            if (response != null && response.getStatus() == PaymentStatus.COMPLETED) break;
        }

        assertNotNull(response);
        if (response.getStatus() == PaymentStatus.COMPLETED) {
            // booking.setPaidAmount(10000.0) should have been called via bookingRepository.save
            verify(bookingRepository, atLeastOnce())
                    .save(argThat(b -> b.getPaidAmount() == 10000.0));
        }
    }
}
