package com.driveaway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * BookingRequest DTO - Data Transfer Object for booking API requests
 * GRASP: DTO Pattern - Separates API layer from internal model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotBlank(message = "Vehicle ID cannot be blank")
    private String vehicleId;

    @NotNull(message = "Start date cannot be null")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date cannot be null")
    @FutureOrPresent(message = "End date must be today or in the future")
    private LocalDate endDate;
}