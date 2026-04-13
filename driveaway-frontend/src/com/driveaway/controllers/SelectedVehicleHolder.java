// src/com/driveaway/controllers/SelectedVehicleHolder.java
package com.driveaway.controllers;

public class SelectedVehicleHolder {
    private static String selectedVehicleId;
    private static String selectedVehicleInfo;
    private static Object selectedVehicleData;

    public static String getSelectedVehicleId() {
        return selectedVehicleId;
    }
    public static void setSelectedVehicleId(String val) {
        selectedVehicleId = val;
    }

    public static String getSelectedVehicleInfo() {
        return selectedVehicleInfo;
    }
    public static void setSelectedVehicleInfo(String info) {
        selectedVehicleInfo = info;
    }

    public static Object getSelectedVehicleData() {
        return selectedVehicleData;
    }
    public static void setSelectedVehicleData(Object data) {
        selectedVehicleData = data;
    }
}