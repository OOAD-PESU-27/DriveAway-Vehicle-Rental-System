package com.driveaway.service.pricing;
import java.time.LocalDate;
import java.util.List;

public interface PricingStrategy {
    double calculate(double baseFare, List<LocalDate> dates, List<String> holidays);
}