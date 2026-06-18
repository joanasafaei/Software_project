package com.parking.manager.service;

import com.parking.manager.database.DatabaseManager;
import com.parking.manager.model.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * سرویس اصلی مدیریت پارکینگ
 * شامل ثبت ورود/خروج، محاسبه هزینه، مدیریت مکان خودروها و نمایش ظرفیت لحظه‌ای
 */
public class ParkingService {

    /**
     * حداکثر ظرفیت پارکینگ (مطابق با تعداد مکان‌های تعریف شده در جدول parking_spots)
     */
    public int getMaxCapacity() throws SQLException {
        return DatabaseManager.getAllParkingSpots().size();
    }

    /**
     * تعداد خودروهای داخل پارکینگ (همان تعداد نشست‌های فعال)
     */
    public int getCurrentOccupancy() throws SQLException {
        return DatabaseManager.getAllActiveSessions().size();
    }

    /**
     * ثبت ورود وسیله نقلیه – همراه با تخصیص یک جای پارک خالی
     * @param plateNumber پلاک
     * @param type نوع وسیله
     * @return جای پارک تخصیص داده شده (برای نمایش به کاربر)
     */
    public ParkingSpot registerEntry(String plateNumber, VehicleType type) throws SQLException {
        // 1. بررسی ظرفیت
        if (getCurrentOccupancy() >= getMaxCapacity()) {
            throw new RuntimeException("پارکینگ پر است! امکان ورود وجود ندارد.");
        }

        // 2. یافتن اولین جای پارک خالی
        List<ParkingSpot> allSpots = DatabaseManager.getAllParkingSpots();
        ParkingSpot freeSpot = allSpots.stream()
                .filter(spot -> !spot.isOccupied())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("جای پارک خالی یافت نشد!"));

        // 3. ثبت در جدول active_sessions
        LocalDateTime now = LocalDateTime.now();
        DatabaseManager.addActiveSession(plateNumber, type, now);

        // 4. تخصیص مکان به خودرو
        DatabaseManager.assignSpotToVehicle(plateNumber, freeSpot.getSpotNumber());

        // 5. به‌روزرسانی اطلاعات جای پارک در شیء برگشتی
        freeSpot.setOccupied(true);
        freeSpot.setCurrentPlateNumber(plateNumber);
        freeSpot.setOccupiedSince(now.toString());
        return freeSpot;
    }

    /**
     * ثبت خروج وسیله، محاسبه هزینه و صدور رسید
     * @return رسید دیجیتال
     */
    public ParkingTicket registerExit(String plateNumber, String operatorUsername) throws SQLException {
        // 1. یافتن نشست فعال
        List<ParkingSession> activeSessions = DatabaseManager.getAllActiveSessions();
        ParkingSession session = activeSessions.stream()
                .filter(s -> s.getVehicle().getPlateNumber().equals(plateNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("وسیله‌ای با این پلاک در پارکینگ نیست!"));

        LocalDateTime exitTime = LocalDateTime.now();
        long durationMinutes = ChronoUnit.MINUTES.between(session.getEntryTime(), exitTime);

        // 2. دریافت تعرفه مناسب
        Tariff tariff = DatabaseManager.getTariff(session.getVehicle().getType().name());
        if (tariff == null) {
            throw new RuntimeException("تعرفه برای این نوع وسیله تعریف نشده است.");
        }

        // 3. محاسبه هزینه
        double cost = calculateCost(tariff, durationMinutes);

        // 4. حذف از نشست‌های فعال
        DatabaseManager.removeActiveSession(plateNumber);

        // 5. آزاد کردن جای پارک
        DatabaseManager.freeSpotByPlate(plateNumber);

        // 6. ساخت رسید و ذخیره در تاریخچه
        ParkingTicket ticket = new ParkingTicket(
                session.getVehicle(),
                exitTime,
                durationMinutes,
                cost,
                UUID.randomUUID().toString()
        );

        // ذخیره در تاریخچه با اپراتور
        DatabaseManager.addToHistory(ticket, operatorUsername);
        return ticket;
    }

    /**
     * محاسبه هزینه بر اساس تعرفه و مدت توقف (دقیقه)
     * منطق: ساعت اول هزینه ثابت، ساعات بعد هزینه متفاوت، و سقف شبانه‌روزی
     */

    private double calculateCost(Tariff tariff, long minutes) {
        final long MINUTES_PER_DAY = 24 * 60;
        long fullDays = minutes / MINUTES_PER_DAY;
        long remainingMinutes = minutes % MINUTES_PER_DAY;

        double cost = fullDays * tariff.getDailyMaxCost();

        if (remainingMinutes > 0) {
            double hours = Math.ceil(remainingMinutes / 60.0);
            if (hours <= 1) {
                cost += tariff.getFirstHourCost();
            } else {
                cost += tariff.getFirstHourCost() + (hours - 1) * tariff.getAdditionalHourCost();
            }
        }

        // اگر مدت کمتر از یک روز بود و هزینه محاسبه شده از سقف روزانه بیشتر شد، آن را محدود کن
        if (fullDays == 0 && cost > tariff.getDailyMaxCost()) {
            cost = tariff.getDailyMaxCost();
        }

        return cost;
    }

    /**
     * دریافت لیست خودروهای داخل پارکینگ (برای نمایش در UI)
     */
    public List<ParkingSession> getActiveSessions() throws SQLException {
        return DatabaseManager.getAllActiveSessions();
    }

    /**
     * دریافت همه جایگاه‌های پارکینگ با وضعیت اشغال
     */
    public List<ParkingSpot> getAllParkingSpots() throws SQLException {
        return DatabaseManager.getAllParkingSpots();
    }

    /**
     * به‌روزرسانی تعرفه (فقط OWNER یا ADMIN)
     */
    public void updateTariff(VehicleType type, Tariff newTariff, Role requesterRole) throws SQLException {
        if (requesterRole != Role.OWNER && requesterRole != Role.ADMIN) {
            throw new SecurityException("فقط مالک یا ادمین می‌توانند تعرفه را تغییر دهند.");
        }
        DatabaseManager.updateTariff(type, newTariff);
    }

    /**
     * دریافت تعرفه جاری برای نوع وسیله
     */
    public Tariff getTariff(VehicleType type) throws SQLException {
        return DatabaseManager.getTariff(type.name());
    }
}