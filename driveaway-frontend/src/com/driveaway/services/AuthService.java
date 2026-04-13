package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

/**
 * AuthService (Frontend) — login and register HTTP calls.
 * Matches backend AuthController at /auth/login and /auth/register.
 */
public class AuthService {

    private static final String BASE_URL = "http://localhost:8080";

    public String login(String email, String password) {
        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        return HttpUtil.sendPost(BASE_URL + "/auth/login", json);
    }

    public String register(String name, String email, String password, String phone) {
        String json = "{\"name\":\"" + name + "\","
                    + "\"email\":\"" + email + "\","
                    + "\"password\":\"" + password + "\","
                    + "\"phone\":\"" + phone + "\"}";
        return HttpUtil.sendPost(BASE_URL + "/auth/register", json);
    }
}