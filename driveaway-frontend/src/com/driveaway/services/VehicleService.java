package com.driveaway.services;

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

    public String getAvailableVehicles(String start, String end) {
    return ApiClient.get(
        "/vehicles/available?startDate=" + start + "&endDate=" + end
    );
    }

    public String bookVehicle(String json) {
    return ApiClient.post("/dates/create", json);
    }
}