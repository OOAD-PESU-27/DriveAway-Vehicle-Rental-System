package com.driveaway.service;

import com.driveaway.entity.Vehicle;
import com.driveaway.exception.ResourceNotFoundException;
import com.driveaway.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VehicleService – covers admin CRUD operations (add, update, delete vehicle).
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        sampleVehicle = new Vehicle();
        sampleVehicle.setId("v-001");
        sampleVehicle.setBrand("Toyota");
        sampleVehicle.setModel("Innova");
        sampleVehicle.setVehicleType("SUV");
        sampleVehicle.setPricePerDay(1500.0);
    }

    // ── addVehicle ────────────────────────────────────────────────────────────

    @Test
    void addVehicle_setsAvailableTrueAndStatusAvailable() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> {
            Vehicle v = inv.getArgument(0);
            v.setId("v-new");
            return v;
        });

        Vehicle input = new Vehicle();
        input.setBrand("Honda");
        input.setModel("City");
        input.setVehicleType("SEDAN");
        input.setPricePerDay(1200.0);

        Vehicle result = vehicleService.addVehicle(input, "admin-1");

        assertTrue(result.isAvailable(), "New vehicle must be marked available");
        assertEquals("AVAILABLE", result.getStatus(), "New vehicle status must be AVAILABLE");
        assertEquals("v-new", result.getId());
    }

    @Test
    void addVehicle_persistsToRepository() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        vehicleService.addVehicle(sampleVehicle, "admin-1");

        verify(vehicleRepository).save(sampleVehicle);
    }

    @Test
    void addVehicle_logsAuditAction() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> {
            Vehicle v = inv.getArgument(0);
            v.setId("v-audit");
            return v;
        });

        vehicleService.addVehicle(sampleVehicle, "admin-1");

        verify(auditLogService).logPaymentAction(eq("VEHICLE_ADDED"), anyString(), eq("admin-1"), anyString());
    }

    // ── updateVehicle ─────────────────────────────────────────────────────────

    @Test
    void updateVehicle_updatesFieldsAndSaves() {
        Vehicle existing = buildVehicle("v-001", "Toyota", "Innova", "SUV", 1500.0);

        Vehicle updates = new Vehicle();
        updates.setBrand("Toyota");
        updates.setModel("Fortuner");
        updates.setVehicleType("SUV");
        updates.setPricePerDay(2500.0);

        when(vehicleRepository.findById("v-001")).thenReturn(Optional.of(existing));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.updateVehicle("v-001", updates, "admin-1");

        assertEquals("Fortuner", result.getModel());
        assertEquals(2500.0, result.getPricePerDay(), 0.001);
        verify(vehicleRepository).save(argThat(v -> "Fortuner".equals(v.getModel()) && v.getPricePerDay() == 2500.0));
    }

    @Test
    void updateVehicle_logsAuditAction() {
        Vehicle existing = buildVehicle("v-001", "Toyota", "Innova", "SUV", 1500.0);
        Vehicle updates = buildVehicle(null, "Toyota", "Fortuner", "SUV", 2500.0);

        when(vehicleRepository.findById("v-001")).thenReturn(Optional.of(existing));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        vehicleService.updateVehicle("v-001", updates, "admin-1");

        verify(auditLogService).logPaymentAction(eq("VEHICLE_UPDATED"), eq("v-001"), eq("admin-1"), anyString());
    }

    @Test
    void updateVehicle_whenNotFound_throwsResourceNotFoundException() {
        when(vehicleRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> vehicleService.updateVehicle("bad-id", sampleVehicle, "admin-1"));
    }

    // ── deleteVehicle ─────────────────────────────────────────────────────────

    @Test
    void deleteVehicle_deletesFromRepository() {
        when(vehicleRepository.findById("v-001")).thenReturn(Optional.of(sampleVehicle));

        vehicleService.deleteVehicle("v-001", "admin-1");

        verify(vehicleRepository).delete(sampleVehicle);
    }

    @Test
    void deleteVehicle_logsAuditAction() {
        when(vehicleRepository.findById("v-001")).thenReturn(Optional.of(sampleVehicle));

        vehicleService.deleteVehicle("v-001", "admin-1");

        verify(auditLogService).logPaymentAction(eq("VEHICLE_DELETED"), eq("v-001"), eq("admin-1"), anyString());
    }

    @Test
    void deleteVehicle_whenNotFound_throwsResourceNotFoundException() {
        when(vehicleRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> vehicleService.deleteVehicle("missing", "admin-1"));
    }

    // ── getAllVehicles / getAvailableVehicles ──────────────────────────────────

    @Test
    void getAllVehicles_returnsAllFromRepository() {
        List<Vehicle> vehicles = List.of(
                buildVehicle("v-1", "Toyota", "Camry", "SEDAN", 1000.0),
                buildVehicle("v-2", "Honda", "City", "SEDAN", 900.0));
        when(vehicleRepository.findAll()).thenReturn(vehicles);

        List<Vehicle> result = vehicleService.getAllVehicles();

        assertEquals(2, result.size());
    }

    @Test
    void markVehicleAsBooked_setsUnavailableAndBookedStatus() {
        Vehicle vehicle = buildVehicle("v-bk", "Maruti", "Swift", "SEDAN", 800.0);
        vehicle.setAvailable(true);
        vehicle.setStatus("AVAILABLE");

        when(vehicleRepository.findById("v-bk")).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        vehicleService.markVehicleAsBooked("v-bk");

        ArgumentCaptor<Vehicle> cap = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(cap.capture());
        assertFalse(cap.getValue().isAvailable());
        assertEquals("BOOKED", cap.getValue().getStatus());
    }

    @Test
    void markVehicleAsAvailable_setsAvailableAndAvailableStatus() {
        Vehicle vehicle = buildVehicle("v-av", "Hyundai", "Creta", "SUV", 1200.0);
        vehicle.setAvailable(false);
        vehicle.setStatus("BOOKED");

        when(vehicleRepository.findById("v-av")).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        vehicleService.markVehicleAsAvailable("v-av");

        ArgumentCaptor<Vehicle> cap = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(cap.capture());
        assertTrue(cap.getValue().isAvailable());
        assertEquals("AVAILABLE", cap.getValue().getStatus());
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Vehicle buildVehicle(String id, String brand, String model, String type, double price) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setBrand(brand);
        v.setModel(model);
        v.setVehicleType(type);
        v.setPricePerDay(price);
        v.setAvailable(true);
        v.setStatus("AVAILABLE");
        return v;
    }
}
