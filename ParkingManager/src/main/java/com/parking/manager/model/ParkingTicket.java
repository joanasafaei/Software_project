package com.parking.manager.model;

import java.time.LocalDateTime;

/**
 * رسید دیجیتال که پس از خروج وسیله صادر می‌شود
 */
public class ParkingTicket {
    private Vehicle vehicle;
    private LocalDateTime exitTime;
    private long durationMinutes;   // مدت توقف بر حسب دقیقه
    private double totalCost;
    private String receiptId;       // شناسه یکتا (UUID)

    public ParkingTicket(Vehicle vehicle, LocalDateTime exitTime, long durationMinutes, double totalCost, String receiptId) {
        this.vehicle = vehicle;
        this.exitTime = exitTime;
        this.durationMinutes = durationMinutes;
        this.totalCost = totalCost;
        this.receiptId = receiptId;
    }

    // فقط Getter (رسید پس از ایجاد تغییر نمی‌کند)
    public Vehicle getVehicle() { return vehicle; }
    public LocalDateTime getExitTime() { return exitTime; }
    public long getDurationMinutes() { return durationMinutes; }
    public double getTotalCost() { return totalCost; }
    public String getReceiptId() { return receiptId; }
}