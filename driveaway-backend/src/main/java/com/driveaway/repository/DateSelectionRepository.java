package com.driveaway.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.driveaway.entity.DateSelectionEntity;

public interface DateSelectionRepository extends MongoRepository<DateSelectionEntity, String> {

    @Query("{ vehicleId: ?0, status: 'CONFIRMED', " +
           "$or: [ { startDate: { $lte: ?2 }, endDate: { $gte: ?1 } } ] }")
    List<DateSelectionEntity> findOverlappingDates(String vehicleId, String start, String end);
}