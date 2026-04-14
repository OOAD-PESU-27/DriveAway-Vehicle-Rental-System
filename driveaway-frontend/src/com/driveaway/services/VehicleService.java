package com.driveaway.services;

import java.time.LocalDate;

import com.driveaway.utils.ApiClient;

public class VehicleService {

    public String getVehicles() {
        return ApiClient.get("/api/v1/vehicles");
    }

    public String getAllVehicles() {
        return ApiClient.get("/api/v1/vehicles");
    }

    public String getAvailableVehicles() {
        return ApiClient.get("/api/v1/vehicles/available");
    }

    public String getPrice(double basePrice, int days, String date) {
        return ApiClient.get(
            "/api/v1/vehicles/price?basePrice=" + basePrice +
            "&days=" + days +
            "&date=" + date
        );
    }

    public String getVehiclesWithPricing(String start, String end) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        StringBuilder datesArray = new StringBuilder("[");
        
        while (!startDate.isAfter(endDate)) {
            if (datesArray.length() > 1) datesArray.append(",");
            datesArray.append("\"").append(startDate.toString()).append("\"");
            startDate = startDate.plusDays(1);
        }
        datesArray.append("]");

        String body = "{\"dates\":" + datesArray.toString() + "}";
        System.out.println("➡ Calling backend /vehicles/search");
        return ApiClient.post("/api/v1/vehicles/search", body);
    }

    public String bookVehicle(String json) {
        return ApiClient.post("/api/v1/dates/create", json);
    }
}