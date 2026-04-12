package com.driveaway.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiClient {

    public static String get(String endpoint) {
        try {
            String baseUrl = "http://localhost:8080"; // change if needed
            URL url = new URL(baseUrl + endpoint);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String post(String endpoint, String json) {
        return HttpUtil.sendPost("http://localhost:8080" + endpoint, json);
    }
    
}