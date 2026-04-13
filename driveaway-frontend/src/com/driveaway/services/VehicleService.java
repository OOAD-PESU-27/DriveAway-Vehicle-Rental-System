package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

/**
 * VehicleService (Frontend) — fetches real vehicles from the backend.
 *
 * Person 2 owns the backend VehicleController at /api/vehicles.
 * We just call it to get the list of available vehicles for the dropdown.
 *
 * If Person 2's endpoint doesn't exist yet, we fall back to /api/vehicles
 * which we'll add to BookingController as a temporary proxy.
 */
public class VehicleService {

    private static final String BASE_URL = "http://localhost:8080";

    /**
     * Returns JSON array of all available vehicles.
     * e.g. [{"id":"...","brand":"Honda","model":"City","pricePerDay":2200,...},...]
     */
    public String getAvailableVehicles() {
        // Try Person 2's endpoint first
        String response = HttpUtil.sendGet(BASE_URL + "/api/vehicles/available");
        if (response != null && response.startsWith("[")) {
            return response;
        }
        // Fallback: get all vehicles
        response = HttpUtil.sendGet(BASE_URL + "/api/vehicles");
        if (response != null && response.startsWith("[")) {
            return response;
        }
        return null;
    }

    public String getAllVehicles() {
        return HttpUtil.sendGet(BASE_URL + "/api/vehicles");
    }
}