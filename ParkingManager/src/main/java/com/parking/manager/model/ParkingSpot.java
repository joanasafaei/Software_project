package com.parking.manager.model;

/**
 * کلاس جای پارک فیزیکی در پارکینگ
 */

public class ParkingSpot {
    private String spotNumber;          // شماره جای پارک، مثلاً "A12"
    private boolean occupied;           // اشغال شده یا خالی
    private String currentPlateNumber;  // پلاک خودرویی که در این جایگاه است (در صورت اشغال)
    private String occupiedSince;       // زمان اشغال (رشته تاریخ، چون ممکن است مستقیم از دیتابیس بیاید)

    public ParkingSpot(String spotNumber, boolean occupied, String currentPlateNumber, String occupiedSince) {
        this.spotNumber = spotNumber;
        this.occupied = occupied;
        this.currentPlateNumber = currentPlateNumber;
        this.occupiedSince = occupiedSince;
    }

    // Getter و Setter
    public String getSpotNumber() { return spotNumber; }
    public void setSpotNumber(String spotNumber) { this.spotNumber = spotNumber; }

    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    public String getCurrentPlateNumber() { return currentPlateNumber; }
    public void setCurrentPlateNumber(String currentPlateNumber) { this.currentPlateNumber = currentPlateNumber; }

    public String getOccupiedSince() { return occupiedSince; }
    public void setOccupiedSince(String occupiedSince) { this.occupiedSince = occupiedSince; }
}