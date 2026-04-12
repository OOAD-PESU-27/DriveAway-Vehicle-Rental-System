package com.driveaway.service.pricing;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.driveaway.service.DateService;

@Service
public class PricingService {

    public double calculateTotalPrice(
            double basePrice,
            List<LocalDate> dates,
            List<String> holidays
    ) {

        // Step 1: Count special days
        int holidayCount = DateService.countHolidays(dates, holidays);
        int weekendCount = DateService.countWeekends(dates);

        // Step 2: Get strategy from factory
        PricingStrategy strategy =
                PricingFactory.getStrategy(holidayCount, weekendCount);

        // Step 3: Calculate total using strategy
        return strategy.calculate(basePrice, dates, holidays);
    }
}