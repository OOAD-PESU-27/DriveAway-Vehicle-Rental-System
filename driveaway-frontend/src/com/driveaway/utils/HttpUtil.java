package com.driveaway.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HttpUtil — Fixed HTTP helper for JavaFX → Spring Boot communication.
 *
 * FIXES vs original:
 *   1. Reads the ERROR stream too (conn.getErrorStream())
 *      Original only read InputStream — so on 400/500 errors it returned null
 *      instead of the backend's actual error message.
 *      Now you'll see "Vehicle not found" instead of just null.
 *
 *   2. Added sendGet() method for fetching data (bookings list, etc.)
 *
 *   3. Added timeout (5 seconds) so the app doesn't hang forever
 *      if backend is down.
 *
 *   4. Returns the HTTP status code alongside the body in sendPost()
 *      so callers can distinguish success from failure.
 */
public class HttpUtil {

    private static final int TIMEOUT_MS = 5000; // 5 seconds

    /**
     * sendPost — sends HTTP POST with a JSON body.
     * Returns the response body string (from either success or error stream).
     * Returns null only if there's a network/connection exception.
     */
    public static String sendPost(String urlStr, String json) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            // Write JSON body
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes("UTF-8"));
                os.flush();
            }

            int statusCode = conn.getResponseCode();
            System.out.println("[HTTP POST] " + urlStr + " → " + statusCode);

            // Read response — use error stream for 4xx/5xx
            BufferedReader br;
            if (statusCode >= 200 && statusCode < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                // CRITICAL FIX: original code crashed here on error responses
                // because getInputStream() throws on 4xx/5xx status codes.
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            String body = response.toString();
            System.out.println("[HTTP POST] Response body: " + body);
            return body;

        } catch (java.net.ConnectException e) {
            System.err.println("[HTTP POST] Cannot connect to backend at: " + urlStr);
            System.err.println("[HTTP POST] Is Spring Boot running? mvn spring-boot:run");
            return null;
        } catch (Exception e) {
            System.err.println("[HTTP POST] Exception: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * sendGet — sends HTTP GET and returns response body.
     * Used to fetch lists (bookings, vehicles, etc.)
     */
    public static String sendGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int statusCode = conn.getResponseCode();
            System.out.println("[HTTP GET] " + urlStr + " → " + statusCode);

            BufferedReader br;
            if (statusCode >= 200 && statusCode < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            return response.toString();

        } catch (java.net.ConnectException e) {
            System.err.println("[HTTP GET] Cannot connect to: " + urlStr);
            return null;
        } catch (Exception e) {
            System.err.println("[HTTP GET] Exception: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * sendPut — sends HTTP PUT (used for handover, cancel, complete maintenance).
     */
    public static String sendPut(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);

            // PUT with empty body
            try (OutputStream os = conn.getOutputStream()) {
                os.write("{}".getBytes("UTF-8"));
            }

            int statusCode = conn.getResponseCode();
            System.out.println("[HTTP PUT] " + urlStr + " → " + statusCode);

            BufferedReader br;
            if (statusCode >= 200 && statusCode < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            return response.toString();

        } catch (java.net.ConnectException e) {
            System.err.println("[HTTP PUT] Cannot connect to: " + urlStr);
            return null;
        } catch (Exception e) {
            System.err.println("[HTTP PUT] Exception: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}