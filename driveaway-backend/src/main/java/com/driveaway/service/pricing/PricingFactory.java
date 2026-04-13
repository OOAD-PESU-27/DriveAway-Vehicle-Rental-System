package com.driveaway.service.pricing;

public class PricingFactory {
    public static PricingStrategy getStrategy(int holidayCount, int weekendCount) {
        int totalSpecialDays = holidayCount + weekendCount;
        if (holidayCount >= 1 && totalSpecialDays >= 2) {
            return new HolidayPricing();
        }
        else if (weekendCount >= 2) {
            return new WeekendPricing();
        }
        else {
            return new NormalPricing();
        }
    }
}