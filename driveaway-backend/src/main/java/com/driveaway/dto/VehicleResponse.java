package com.driveaway.dto;

public class VehicleResponse {

    private String id;
    private String name;
    private int seatingCapacity;
    private double pricePerDay;
    private double weekendPricePerDay;
    private double holidayPricePerDay;

    private double totalPrice;

    // getters + setters
        public int getSeatingCapacity() {
            return seatingCapacity;
        }

        public void setSeatingCapacity(int seatingCapacity) {
            this.seatingCapacity = seatingCapacity;
        }
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