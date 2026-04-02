package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

public class BookingService {

    private static final String BASE_URL = "http://localhost:8080";

    public String createBooking(String userId, String vehicleId, String startDate, String endDate) {
        String json = String.format(
                "{\"vehicleId\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\"}",
                vehicleId, startDate, endDate
        );
        return HttpUtil.sendPostWithHeader(BASE_URL + "/api/v1/bookings", json, "X-User-ID", userId);
    }

    public String getUserBookings(String userId) {
        return HttpUtil.sendGet(BASE_URL + "/api/v1/bookings/user/" + userId);
    }

    public String cancelBooking(String bookingId, String userId) {
        return HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/bookings/" + bookingId + "/cancel", "{}", "X-User-ID", userId);
    }
}
