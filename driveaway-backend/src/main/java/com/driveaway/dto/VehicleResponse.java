package com.driveaway.dto;

public class VehicleResponse {

    private String id;
    private String name;

    // Vehicle details
    private String vehicleType;
    private String fuelType;
    private String transmission;
    private int seatingCapacity;

    // Availability
    private boolean available;

    // Pricing
    private double pricePerDay;
    private double weekendPricePerDay;
    private double holidayPricePerDay;
    private double totalPrice;
    private String priceBreakdown;


    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }
    public String getPriceBreakdown() {
        return priceBreakdown;
    }

    public void setPriceBreakdown(String priceBreakdown) {
        this.priceBreakdown = priceBreakdown;
    }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(double pricePerDay) { this.pricePerDay = pricePerDay; }
    public double getWeekendPricePerDay() { return weekendPricePerDay; }
    public void setWeekendPricePerDay(double weekendPricePerDay) { this.weekendPricePerDay = weekendPricePerDay; }
    public double getHolidayPricePerDay() { return holidayPricePerDay; }
    public void setHolidayPricePerDay(double holidayPricePerDay) { this.holidayPricePerDay = holidayPricePerDay; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}