package com.driveaway.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "vehicles")
public class Vehicle {

    @Id
    private String id;

    private String brand;
    private String model;
    private String vehicleType;
    private double pricePerDay;
    private String fuelType;
    private String transmission;
    private int seatingCapacity;

    // Getters only (enough for now)

    public String getId() { return id; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getVehicleType() { return vehicleType; }
    public double getPricePerDay() { return pricePerDay; }
    public String getFuelType() { return fuelType; }
    public String getTransmission() { return transmission; }
    public int getSeatingCapacity() { return seatingCapacity; }
    
}