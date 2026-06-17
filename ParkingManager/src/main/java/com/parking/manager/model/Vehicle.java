package com.parking.manager.model;

import java.time.LocalDateTime;

/**
 * اطلاعات یک وسیله نقلیه (خودرو یا موتور)
 */
public class Vehicle {
    private String plateNumber;      // شماره پلاک
    private VehicleType type;        // نوع وسیله
    private LocalDateTime entryTime; // زمان ورود (در زمان ثبت ورود مقداردهی می‌شود)

    // Constructor برای ورود بدون زمان (برای استفاده در تاریخچه)
    public Vehicle(String plateNumber, VehicleType type) {
        this.plateNumber = plateNumber;
        this.type = type;
    }

    // Constructor کامل با زمان ورود
    public Vehicle(String plateNumber, VehicleType type, LocalDateTime entryTime) {
        this(plateNumber, type);
        this.entryTime = entryTime;
    }

    // ---------- Getter و Setter ----------
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public VehicleType getType() { return type; }
    public void setType(VehicleType type) { this.type = type; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }
}