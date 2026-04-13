package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

/**
 * LicenseService (Frontend) — adds/updates driving license via backend.
 * Matches backend LicenseController at /license/add.
 */
public class LicenseService {

    private static final String BASE_URL = "http://localhost:8080";

    public String addLicense(String userId, String licenseNumber, String expiryDate) {
        String json = "{\"userId\":\"" + userId + "\","
                    + "\"licenseNumber\":\"" + licenseNumber + "\","
                    + "\"expiryDate\":\"" + expiryDate + "\"}";
        return HttpUtil.sendPost(BASE_URL + "/license/add", json);
    }
}