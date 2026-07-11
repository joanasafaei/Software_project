package com.parking.manager.model;

/**
 ‏ * نوع وسیله نقلیه
 ‏ */
        public enum VehicleType {
 CAR("خودرو"),
 MOTORCYCLE("موتور");

 private final String displayName;

 VehicleType(String displayName) {
 this.displayName = displayName;
 }

 @Override
 public String toString() {
 return displayName;
 }
}