package com.driveaway.models;

public class Vehicle {

    private String id;
    private String name;
    private String type;
    private double pricePerDay;

    public Vehicle() {}

    public Vehicle(String id, String name, String type, double pricePerDay) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.pricePerDay = pricePerDay;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public double getPricePerDay() {
        return pricePerDay;
    }
}