package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

public class BookingService {

    private static final String BASE_URL = "http://localhost:8080";

    public String createBooking(String userId, String vehicleId, String startDate, String endDate) {
        String json = String.format(
                "{\"vehicleId\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\"}",
                vehicleId, startDate, endDate
        );

        return HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/bookings",
                json,
                "X-User-ID",
                userId
        );
    }

    public String getUserBookings(String userId) {
        return HttpUtil.sendGet(
                BASE_URL + "/api/v1/bookings/user/" + userId
        );
    }

    public String cancelBooking(String bookingId, String userId) {
        return HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/bookings/" + bookingId + "/cancel",
                "{}",
                "X-User-ID",
                userId
        );
    }

    public String handoverVehicle(String bookingId) {
        return HttpUtil.sendPut(
                BASE_URL + "/api/v1/bookings/" + bookingId + "/handover",
                "{}"
        );
    }

    public String returnVehicle(String bookingId) {
        return HttpUtil.sendPut(
                BASE_URL + "/api/v1/bookings/" + bookingId + "/return",
                "{}"
        );
    }

    public String getBookingsByStatus(String status) {

        String endpoint =
            BASE_URL + "/api/v1/bookings/status/" + status;

         return HttpUtil.sendGet(endpoint);
    }
    public String returnInspection(String jsonBody) {

        String endpoint =
            BASE_URL + "/api/v1/bookings/return-inspection";

         return HttpUtil.sendPost(endpoint, jsonBody);
    }
    public String getScheduledMaintenance() {

        String endpoint =
            BASE_URL + "/api/v1/bookings/maintenance/scheduled";

        return HttpUtil.sendGet(endpoint);
    }
    public String completeMaintenance(String maintenanceId) {

        String endpoint =
            BASE_URL + "/api/v1/bookings/maintenance/complete/" + maintenanceId;

        return HttpUtil.sendPost(endpoint, "{}");
}
}