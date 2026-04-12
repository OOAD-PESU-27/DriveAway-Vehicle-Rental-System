package com.driveaway.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {

    public static String sendPost(String urlStr, String json) {
    try {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // 🔥 IMPORTANT HEADERS
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes());
        os.flush();
        os.close();

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
}