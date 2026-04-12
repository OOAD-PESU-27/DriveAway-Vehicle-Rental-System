package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

public class VehicleService {

    private static final String BASE_URL = "http://localhost:8080";

    public String getAvailableVehicles() {
        return HttpUtil.sendGet(BASE_URL + "/api/v1/vehicles/available");
    }

    public String getAvailableVehiclesByType(String type) {
        return HttpUtil.sendGet(BASE_URL + "/api/v1/vehicles/available?type=" + type);
    }

    public String getAllVehicles() {
        return HttpUtil.sendGet(BASE_URL + "/api/v1/vehicles");
    }
}
