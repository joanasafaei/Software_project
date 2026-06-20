package com.parking.manager.model;

/**
 * ثبت شیفت کاری هر اپراتور
 */

public class Shift {
    private int id;
    private String operatorUsername;
    private String startTime;   // به فرم ISO e.g. "2025-04-30T08:00:00"
    private String endTime;     // می‌تواند null تا زمانی که شیفت بسته نشده
    private boolean active;

    public Shift(int id, String operatorUsername, String startTime, String endTime, boolean active) {
        this.id = id;
        this.operatorUsername = operatorUsername;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
    }

    // Getter و Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}