package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

public class LicenseService {

    private static final String BASE_URL = "http://localhost:8080";

    public String addLicense(String userId, String number, String expiry) {

        String json = String.format(
                "{\"userId\":\"%s\",\"licenseNumber\":\"%s\",\"expiryDate\":\"%s\"}",
                userId, number, expiry
        );

        return HttpUtil.sendPost(BASE_URL + "/license/add", json);
    }
}