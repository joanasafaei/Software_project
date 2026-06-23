package com.parking.manager.model;

import java.time.LocalDate;

/**
 * ذخیره خلاصه آمار هر روز برای گزارش‌های روزانه، هفتگی، ماهانه
 */
public class DailyStat {
    private LocalDate date;
    private int totalEntries;      // تعداد ورودی‌ها در آن روز
    private int totalExits;        // تعداد خروجی‌ها
    private int totalIncome;       // درآمد کل (ریال)
    private double avgOccupancyRate; // میانگین درصد اشغال پارکینگ در آن روز

    public DailyStat(LocalDate date, int totalEntries, int totalExits, int totalIncome, double avgOccupancyRate) {
        this.date = date;
        this.totalEntries = totalEntries;
        this.totalExits = totalExits;
        this.totalIncome = totalIncome;
        this.avgOccupancyRate = avgOccupancyRate;
    }

    // Getter و Setter
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getTotalEntries() { return totalEntries; }
    public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }

    public int getTotalExits() { return totalExits; }
    public void setTotalExits(int totalExits) { this.totalExits = totalExits; }

    public int getTotalIncome() { return totalIncome; }
    public void setTotalIncome(int totalIncome) { this.totalIncome = totalIncome; }

    public double getAvgOccupancyRate() { return avgOccupancyRate; }
    public void setAvgOccupancyRate(double avgOccupancyRate) { this.avgOccupancyRate = avgOccupancyRate; }
}