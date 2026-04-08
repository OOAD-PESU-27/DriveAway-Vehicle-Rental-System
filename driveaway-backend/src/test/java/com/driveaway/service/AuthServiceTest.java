package com.driveaway.service;

import com.driveaway.dto.RegisterRequest;
import com.driveaway.entity.User;
import com.driveaway.exception.DuplicateEmailException;
import com.driveaway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService – covers registration duplicate-email handling and email normalisation.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder encoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.name = "Alice";
        validRequest.email = "Alice@Example.COM";   // intentionally un-normalised
        validRequest.password = "SecurePass1";
        validRequest.phone = "9876543210";
    }

    // ── first-time registration ──────────────────────────────────────────────

    @Test
    void register_newUser_succeeds() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(encoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = authService.register(validRequest);

        assertEquals("Registered Successfully", result);
    }

    @Test
    void register_normaliseEmail_savesLowerCaseAndTrimmed() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(encoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(validRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("alice@example.com", captor.getValue().getEmail());
    }

    // ── duplicate email ──────────────────────────────────────────────────────

    @Test
    void register_duplicateEmail_throwsDuplicateEmailException() {
        User existing = new User();
        existing.setEmail("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateEmailException.class, () -> authService.register(validRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmailMessage_isClear() {
        User existing = new User();
        existing.setEmail("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> authService.register(validRequest));
        assertTrue(ex.getMessage().toLowerCase().contains("already registered"),
                "Message should indicate the email is already registered");
    }

    @Test
    void register_raceConditionDuplicateKey_throwsDuplicateEmailException() {
        // Simulates the case where findByEmail returns empty (no duplicate)
        // but the save throws DuplicateKeyException due to a concurrent request
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(encoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("duplicate key"));

        assertThrows(DuplicateEmailException.class, () -> authService.register(validRequest));
    }

    // ── generic errors are not misreported as duplicate email ────────────────

    @Test
    void register_unexpectedDbError_propagatesAsRuntimeException_notDuplicateEmail() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(encoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("connection refused"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(validRequest));
        assertFalse(ex instanceof DuplicateEmailException,
                "A generic DB error must NOT be reported as duplicate email");
    }

    // ── null / blank email guard ─────────────────────────────────────────────

    @Test
    void register_nullEmail_throwsIllegalArgumentException() {
        validRequest.email = null;
        assertThrows(IllegalArgumentException.class, () -> authService.register(validRequest));
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_blankEmail_throwsIllegalArgumentException() {
        validRequest.email = "   ";
        assertThrows(IllegalArgumentException.class, () -> authService.register(validRequest));
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }
}
