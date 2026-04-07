package com.driveaway.service;

import com.driveaway.PaymentStatus;
import com.driveaway.dto.PaymentRequest;
import com.driveaway.dto.PaymentResponse;
import com.driveaway.entity.Payment;
import com.driveaway.exception.PaymentException;
import com.driveaway.repository.PaymentRepository;
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
}
