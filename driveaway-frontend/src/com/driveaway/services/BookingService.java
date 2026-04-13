package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

/**
 * BookingService (Frontend) — all HTTP calls related to bookings.
 *
 * Maps to backend BookingController endpoints:
 *   POST   /api/bookings?licenseVerified=true  → createBooking()
 *   GET    /api/bookings                        → getAllBookings()
 *   GET    /api/bookings/{id}                   → getBookingById()
 *   PUT    /api/bookings/{id}/handover          → handoverVehicle()
 *   PUT    /api/bookings/{id}/cancel            → cancelBooking()
 *   POST   /api/bookings/return                 → returnInspection()
 *   GET    /api/bookings/maintenance/all        → getAllMaintenance()
 *   GET    /api/bookings/maintenance/scheduled  → getScheduledMaintenance()
 *   PUT    /api/bookings/maintenance/{id}/complete → completeMaintenance()
 *   GET    /api/bookings/damages/{bookingId}    → getDamagesByBooking()
 */
public class BookingService {

    private static final String BASE_URL = "http://localhost:8080";

    // ── CREATE BOOKING ────────────────────────────────────────────────────────
    public String createBooking(String jsonBody, boolean licenseVerified) {
        String url = BASE_URL + "/api/bookings?licenseVerified=" + licenseVerified;
        return HttpUtil.sendPost(url, jsonBody);
    }

    // ── GET ALL BOOKINGS ───────────────────────────────────────────────────────
    public String getAllBookings() {
        return HttpUtil.sendGet(BASE_URL + "/api/bookings");
    }

    // ── GET BY STATUS (e.g. "CONFIRMED", "HANDED_OVER") ──────────────────────
    public String getBookingsByStatus(String status) {
        return HttpUtil.sendGet(BASE_URL + "/api/bookings/status/" + status);
    }

    // ── GET BY USER ───────────────────────────────────────────────────────────
    public String getBookingsByUser(String userId) {
        return HttpUtil.sendGet(BASE_URL + "/api/bookings/user/" + userId);
    }

    // ── HANDOVER VEHICLE (CONFIRMED → HANDED_OVER) ────────────────────────────
    public String handoverVehicle(String bookingId) {
        return HttpUtil.sendPut(BASE_URL + "/api/bookings/" + bookingId + "/handover");
    }

    // ── CANCEL BOOKING ────────────────────────────────────────────────────────
    public String cancelBooking(String bookingId) {
        return HttpUtil.sendPut(BASE_URL + "/api/bookings/" + bookingId + "/cancel");
    }

    // ── RETURN + INSPECTION (KEY ENDPOINT) ────────────────────────────────────
    public String returnInspection(String jsonBody) {
        String url = BASE_URL + "/api/bookings/return";
        return HttpUtil.sendPost(url, jsonBody);
    }

    // ── MAINTENANCE ───────────────────────────────────────────────────────────
    public String getAllMaintenance() {
        return HttpUtil.sendGet(BASE_URL + "/api/bookings/maintenance/all");
    }

    public String getScheduledMaintenance() {
        return HttpUtil.sendGet(BASE_URL + "/api/bookings/maintenance/scheduled");
    }

    public String completeMaintenance(String maintenanceId) {
        return HttpUtil.sendPut(BASE_URL + "/api/bookings/maintenance/" + maintenanceId + "/complete");
    }

    // ── DAMAGES ───────────────────────────────────────────────────────────────
    public String getDamagesByBooking(String bookingId) {
        return HttpUtil.sendGet(BASE_URL + "/api/bookings/damages/" + bookingId);
    }
}