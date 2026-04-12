package com.driveaway.service.pricing;

import java.time.LocalDate;
import java.util.List;

public class HolidayPricing implements PricingStrategy {

    public double calculate(double base, List<LocalDate> dates, List<String> holidays) {

        double total = 0;

        for (LocalDate date : dates) {

            if (holidays.contains(date.toString())) {
                total += base * 1.5;
            } 
            else if (date.getDayOfWeek().getValue() >= 6) {
                total += base * 1.3;
            } 
            else {
                total += base;
            }
        }

        return total;
    }
}