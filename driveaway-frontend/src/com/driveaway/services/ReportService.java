package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

public class ReportService {

    private static final String BASE_URL = "http://localhost:8080";

    public String getAllReports(String adminId) {
        return HttpUtil.sendGetWithHeader(BASE_URL + "/api/v1/admin/reports", "X-Admin-ID", adminId);
    }

    public String generateDailyReport(String adminId) {
        return HttpUtil.sendGetWithHeader(BASE_URL + "/api/v1/admin/reports/daily", "X-Admin-ID", adminId);
    }

    public String generateWeeklyReport(String adminId) {
        return HttpUtil.sendGetWithHeader(BASE_URL + "/api/v1/admin/reports/weekly", "X-Admin-ID", adminId);
    }

    public String generateMonthlyReport(String adminId) {
        return HttpUtil.sendGetWithHeader(BASE_URL + "/api/v1/admin/reports/monthly", "X-Admin-ID", adminId);
    }

    public String generateVehicleReport(String adminId) {
        return HttpUtil.sendGetWithHeader(BASE_URL + "/api/v1/admin/reports/vehicles", "X-Admin-ID", adminId);
    }

    public String generateRevenueReport(String adminId) {
        return HttpUtil.sendGetWithHeader(BASE_URL + "/api/v1/admin/reports/revenue", "X-Admin-ID", adminId);
    }

    public String generateVehicleUsageReport(String adminId) {
        return HttpUtil.sendGetWithHeader(BASE_URL + "/api/v1/admin/reports/vehicle-usage", "X-Admin-ID", adminId);
    }

    public String generateDamageReport(String adminId) {
        return HttpUtil.sendGetWithHeader(BASE_URL + "/api/v1/admin/reports/damage", "X-Admin-ID", adminId);
    }

    public String generateMaintenanceReport(String adminId) {
        return HttpUtil.sendGetWithHeader(BASE_URL + "/api/v1/admin/reports/maintenance", "X-Admin-ID", adminId);
    }
}
