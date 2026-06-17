package com.parking.manager.model;

/**
 * تعرفه پارکینگ برای هر نوع وسیله
 */
public class Tariff {
    private VehicleType vehicleType;
    private double firstHourCost;      // هزینه ساعت اول
    private double additionalHourCost; // هزینه هر ساعت اضافی بعد از ساعت اول
    private double dailyMaxCost;       // حداکثر هزینه برای توقف شبانه‌روزی (۲۴ ساعت)

    public Tariff(VehicleType vehicleType, double firstHourCost, double additionalHourCost, double dailyMaxCost) {
        this.vehicleType = vehicleType;
        this.firstHourCost = firstHourCost;
        this.additionalHourCost = additionalHourCost;
        this.dailyMaxCost = dailyMaxCost;
    }

    // Getter و Setter
    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public double getFirstHourCost() { return firstHourCost; }
    public void setFirstHourCost(double firstHourCost) { this.firstHourCost = firstHourCost; }

    public double getAdditionalHourCost() { return additionalHourCost; }
    public void setAdditionalHourCost(double additionalHourCost) { this.additionalHourCost = additionalHourCost; }

    public double getDailyMaxCost() { return dailyMaxCost; }
    public void setDailyMaxCost(double dailyMaxCost) { this.dailyMaxCost = dailyMaxCost; }
}