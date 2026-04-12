package com.driveaway.config;

import com.driveaway.entity.Vehicle;
import com.driveaway.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * VehicleDataSeeder - Seeds sample vehicles into the DB on startup if none exist.
 * Ensures GET /api/v1/vehicles and /api/v1/vehicles/available return data on a fresh setup.
 */
@Component
@RequiredArgsConstructor
public class VehicleDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VehicleDataSeeder.class);

    private final VehicleRepository vehicleRepository;

    @Override
    public void run(String... args) {
        // Only seed if the vehicles collection is empty to avoid duplicates on restart
        if (vehicleRepository.count() > 0) {
            log.info("Vehicles already exist in DB – skipping seed.");
            return;
        }

        List<Vehicle> sampleVehicles = buildSampleVehicles();
        vehicleRepository.saveAll(sampleVehicles);
        log.info("Seeded {} sample vehicles into the database.", sampleVehicles.size());
    }

    private List<Vehicle> buildSampleVehicles() {
        return List.of(
                vehicle("Toyota",   "Camry",    "SEDAN",  2500, "TN01AB1234", "PETROL",  "AUTOMATIC", 5),
                vehicle("Honda",    "City",     "SEDAN",  2200, "TN02CD5678", "PETROL",  "MANUAL",    5),
                vehicle("Hyundai",  "Creta",    "SUV",    3000, "TN03EF9012", "DIESEL",  "AUTOMATIC", 5),
                vehicle("Mahindra", "Thar",     "SUV",    3500, "TN04GH3456", "DIESEL",  "MANUAL",    4),
                vehicle("Tata",     "Nexon EV", "SUV",    2800, "TN05IJ7890", "ELECTRIC","AUTOMATIC", 5),
                vehicle("Maruti",   "Swift",    "HATCHBACK", 1800, "TN06KL2345", "PETROL", "MANUAL",  5),
                vehicle("Ford",     "EcoSport", "SUV",    2600, "TN07MN6789", "PETROL",  "AUTOMATIC", 5),
                vehicle("BMW",      "3 Series", "SEDAN",  6000, "TN08OP1234", "PETROL",  "AUTOMATIC", 5),
                vehicle("Royal Enfield", "Classic 350", "BIKE", 800, "TN09QR5678", "PETROL", "MANUAL", 2),
                vehicle("Honda",    "Activa 6G","SCOOTER", 500, "TN10ST9012", "PETROL",  "AUTOMATIC", 2)
        );
    }

    /** Helper to construct a fully-populated, available Vehicle. */
    private Vehicle vehicle(String brand, String model, String type, double pricePerDay,
                            String regNo, String fuel, String transmission, int seats) {
        Vehicle v = new Vehicle();
        v.setBrand(brand);
        v.setModel(model);
        v.setVehicleType(type);
        v.setPricePerDay(pricePerDay);
        v.setRegistrationNumber(regNo);
        v.setFuelType(fuel);
        v.setTransmission(transmission);
        v.setSeatingCapacity(seats);
        v.setAvailable(true);
        v.setStatus("AVAILABLE");
        return v;
    }
}
