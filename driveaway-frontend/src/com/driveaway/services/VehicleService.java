package com.driveaway.services;

import java.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
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

        JSONArray datesArray = new JSONArray();

        while (!startDate.isAfter(endDate)) {
            datesArray.put(startDate.toString());
            startDate = startDate.plusDays(1);
        }

        JSONObject body = new JSONObject();
        body.put("dates", datesArray);

        return ApiClient.post("/vehicles/search", body.toString());
    }

    public String bookVehicle(String json) {
    return ApiClient.post("/dates/create", json);
    }
}