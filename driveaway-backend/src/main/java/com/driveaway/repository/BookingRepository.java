package com.driveaway.repository;
import com.driveaway.entity.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByUserId(String userId);
    List<Booking> findByVehicleId(String vehicleId);
    List<Booking> findByStatus(String status);
}