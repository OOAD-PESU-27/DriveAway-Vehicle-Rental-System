package com.driveaway.controllers;

public class VehicleController {

    public static class SelectedVehicleHolder {

        // 🔹 Store full vehicle data (array from catalog)
        private static String[] selectedVehicleData;

        // 🔹 Store vehicle ID
        private static String selectedVehicleId;

        // 🔹 Store dates
        private static String selectedStartDate;
        private static String selectedEndDate;

        // 🔹 Optional extra info (for dashboard compatibility)
        private static String selectedVehicleInfo;

        // =========================
        // VEHICLE DATA
        // =========================
        public static void setSelectedVehicleData(String[] data) {
            selectedVehicleData = data;
        }

        public static String[] getSelectedVehicleData() {
            return selectedVehicleData;
        }

        // =========================
        // VEHICLE ID
        // =========================
        public static void setSelectedVehicleId(String id) {
            selectedVehicleId = id;
        }

        public static String getSelectedVehicleId() {
            return selectedVehicleId;
        }

        // =========================
        // DATES
        // =========================
        public static void setSelectedStartDate(String start) {
            selectedStartDate = start;
        }

        public static void setSelectedEndDate(String end) {
            selectedEndDate = end;
        }

        public static String getSelectedStartDate() {
            return selectedStartDate;
        }

        public static String getSelectedEndDate() {
            return selectedEndDate;
        }

        // =========================
        // OPTIONAL VEHICLE INFO (Fix for DashboardController error)
        // =========================
        public static void setSelectedVehicleInfo(String info) {
            selectedVehicleInfo = info;
        }

        public static String getSelectedVehicleInfo() {
            return selectedVehicleInfo;
        }
    }
}