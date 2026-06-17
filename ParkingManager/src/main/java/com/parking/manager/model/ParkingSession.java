package com.parking.manager.model;

import java.time.LocalDateTime;

/**
 * نمایانگر یک وسیله نقلیه که هم‌اکنون در پارکینگ است
 */
public class ParkingSession {
    private Vehicle vehicle;
    private LocalDateTime entryTime;

    public ParkingSession(Vehicle vehicle, LocalDateTime entryTime) {
        this.vehicle = vehicle;
        this.entryTime = entryTime;
    }

    public Vehicle getVehicle() { return vehicle; }
    public LocalDateTime getEntryTime() { return entryTime; }
}
