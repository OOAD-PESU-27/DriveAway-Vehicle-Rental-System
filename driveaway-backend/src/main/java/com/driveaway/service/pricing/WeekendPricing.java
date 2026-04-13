package com.driveaway.service.pricing;
import java.time.LocalDate;
import java.util.List;

public class WeekendPricing implements PricingStrategy {
    public double calculate(double base, List<LocalDate> dates, List<String> holidays) {
        double total = 0;
        for (LocalDate date : dates) {
            if (date.getDayOfWeek().getValue() >= 6) {
                total += base * 1.3;
            } else {
                total += base;
            }
        }
        return total;
    }
}