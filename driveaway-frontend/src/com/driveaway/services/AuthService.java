package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

public class AuthService {

    private static final String BASE_URL = "http://localhost:8080";

    public String login(String email, String password) {

        String json = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}",
                email, password
        );

        return HttpUtil.sendPost(BASE_URL + "/auth/login", json);
    }
    public String register(String name, String email, String password, String phone) {

    String json = String.format(
            "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\",\"phone\":\"%s\"}",
            name, email, password, phone
    );

    return HttpUtil.sendPost(BASE_URL + "/auth/register", json);
}
}