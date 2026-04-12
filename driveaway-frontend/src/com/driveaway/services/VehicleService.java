package com.driveaway.services;

import java.time.LocalDate;
import com.driveaway.utils.ApiClient;

public class VehicleService {

    public String getVehicles() {
        return ApiClient.get("/vehicles");
    }

    public String getAvailableVehicles() {
        return ApiClient.get("/vehicles/available");
    }

    public String getPrice(double basePrice, int days, String date) {
        return ApiClient.get(
            "/vehicles/price?basePrice=" + basePrice +
            "&days=" + days +
            "&date=" + date
        );
    }

    public String getVehiclesWithPricing(String start, String end) {

        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        // Build JSON array of dates manually
        StringBuilder datesArray = new StringBuilder("[");
        
        while (!startDate.isAfter(endDate)) {
            if (datesArray.length() > 1) datesArray.append(",");
            datesArray.append("\"").append(startDate.toString()).append("\"");
            startDate = startDate.plusDays(1);
        }
        datesArray.append("]");

        // Build JSON body manually
        String body = "{\"dates\":" + datesArray.toString() + "}";

        return ApiClient.post("/vehicles/search", body);
    }

    public String bookVehicle(String json) {
        return ApiClient.post("/dates/create", json);
    }
}