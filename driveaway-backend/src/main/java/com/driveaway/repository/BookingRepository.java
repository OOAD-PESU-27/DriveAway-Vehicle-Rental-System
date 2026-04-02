package com.driveaway.repository;

import com.driveaway.entity.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

/**
 * BookingRepository - Handles database operations for bookings
 * GRASP: Repository Pattern - Abstracts database operations
 */
@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    List<Booking> findByVehicleId(String vehicleId);

    List<Booking> findByStatus(String status);

    List<Booking> findByVehicleIdAndStatus(String vehicleId, String status);

    List<Booking> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByVehicleIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String vehicleId, String status, LocalDate endDate, LocalDate startDate);
}