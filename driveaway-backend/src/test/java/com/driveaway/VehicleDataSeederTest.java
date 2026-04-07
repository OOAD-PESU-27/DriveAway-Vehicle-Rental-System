package com.driveaway;

import com.driveaway.config.VehicleDataSeeder;
import com.driveaway.entity.Vehicle;
import com.driveaway.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VehicleDataSeeder – verifies seed-on-empty and no-duplicate-seed behavior.
 */
@ExtendWith(MockitoExtension.class)
class VehicleDataSeederTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleDataSeeder seeder;

    @Test
    void seedsVehiclesWhenRepositoryIsEmpty() throws Exception {
        when(vehicleRepository.count()).thenReturn(0L);

        seeder.run();

        // saveAll must be called once with a non-empty list
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Vehicle>> captor = ArgumentCaptor.forClass(List.class);
        verify(vehicleRepository).saveAll(captor.capture());

        List<Vehicle> saved = captor.getValue();
        assertFalse(saved.isEmpty(), "Seeder should insert at least one vehicle");

        // Every seeded vehicle must be available with status AVAILABLE
        saved.forEach(v -> {
            assertTrue(v.isAvailable(), "Seeded vehicle should be available");
            assertEquals("AVAILABLE", v.getStatus(), "Seeded vehicle status should be AVAILABLE");
            assertNotNull(v.getBrand(), "Brand must not be null");
            assertNotNull(v.getModel(), "Model must not be null");
            assertNotNull(v.getFuelType(), "FuelType must not be null");
            assertNotNull(v.getTransmission(), "Transmission must not be null");
            assertTrue(v.getSeatingCapacity() > 0, "SeatingCapacity must be positive");
        });
    }

    @Test
    void doesNotSeedWhenVehiclesAlreadyExist() throws Exception {
        when(vehicleRepository.count()).thenReturn(5L);

        seeder.run();

        // saveAll must never be called if records already exist
        verify(vehicleRepository, never()).saveAll(anyList());
    }
}
