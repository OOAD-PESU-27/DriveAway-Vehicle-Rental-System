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
        int holidayCount = DateService.countHolidays(dates, holidays);
        int weekendCount = DateService.countWeekends(dates);

        PricingStrategy strategy =
                PricingFactory.getStrategy(holidayCount, weekendCount);
        return strategy.calculate(basePrice, dates, holidays);
    }
}