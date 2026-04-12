package com.driveaway.service.pricing;

import java.time.LocalDate;
import java.util.List;

public class NormalPricing implements PricingStrategy {

    public double calculate(double base, List<LocalDate> dates, List<String> holidays) {
        return base * dates.size();
    }
}