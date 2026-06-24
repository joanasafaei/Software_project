package com.parking.manager.service;

import com.parking.manager.database.DatabaseManager;
import com.parking.manager.model.DailyStat;
import com.parking.manager.model.ParkingTicket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * سرویس گزارش‌گیری
 * شامل گزارش‌های روزانه، هفتگی، ماهانه از درآمد، تعداد ورود/خروج، اشغال فضا و عملکرد اپراتورها
 */
public class ReportService {

    // ---------- گزارش بر اساس بازه زمانی (از تاریخچه) ----------
    public List<ParkingTicket> getTicketsBetween(LocalDate from, LocalDate to) throws SQLException {
        return DatabaseManager.getHistoryByDateRange(from, to);
    }

    /**
     * خلاصه آماری روزانه شامل تعداد ورود، خروج، درآمد و میانگین اشغال
     * (برای روزهای معین – از جدول daily_stats استفاده می‌کند یا در صورت نبود محاسبه می‌کند)
     */

    public DailyStat getDailyStat(LocalDate date) throws SQLException {
        // تعداد ورود از تاریخچه (خودروهایی که خروج کرده‌اند)
        List<ParkingTicket> tickets = DatabaseManager.getHistoryByDateRange(date, date);
        int historicalEntries = tickets.size();

        // تعداد ورود از نشست‌های فعال (خودروهایی که هنوز داخل پارکینگ هستند)
        int activeEntries = DatabaseManager.getActiveEntriesCountForDate(date);

        int totalEntries = historicalEntries + activeEntries;  // ورود کل
        int exits = (int) tickets.stream().filter(t -> t.getExitTime() != null).count();
        int income = (int) tickets.stream().mapToDouble(ParkingTicket::getTotalCost).sum();
        double avgOccupancy = calculateAvgOccupancy(date); // یا 0.0 اگر نمی‌خواهید

        return new DailyStat(date, totalEntries, exits, income, avgOccupancy);
    }

    // متد جدید برای محاسبه میانگین اشغال در یک روز خاص
    private double calculateAvgOccupancy(LocalDate date) throws SQLException {
        int maxCapacity = DatabaseManager.getAllParkingSpots().size();
        if (maxCapacity == 0) return 0.0;

        LocalDateTime startOfDay = date.atStartOfDay();
        long totalOccupancy = 0;
        int samples = 0;

        // نمونه‌برداری هر 2 ساعت (12 نمونه) برای کاهش بار، اما می‌توانید هر ساعت 24 نمونه بگیرید
        for (int hour = 0; hour < 24; hour += 2) {
            LocalDateTime sampleTime = startOfDay.plusHours(hour);
            // شمارش خودروهایی که در این لحظه داخل پارکینگ بودند
            String sql = "SELECT COUNT(*) FROM parking_history WHERE entry_time <= ? AND (exit_time > ? OR exit_time IS NULL)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, sampleTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                pstmt.setString(2, sampleTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    totalOccupancy += rs.getInt(1);
                    samples++;
                }
            }
        }
        if (samples == 0) return 0.0;
        double avg = (double) totalOccupancy / samples;
        return (avg / maxCapacity) * 100.0;
    }

    // تخمین میانگین اشغال در یک روز (با نمونه‌گیری هر ساعت – ساده شده)
    private double estimateAvgOccupancy(LocalDate date) throws SQLException {
        int totalMinutes = 0;
        int maxCapacity = DatabaseManager.getAllParkingSpots().size();
        if (maxCapacity == 0) return 0.0;
        // به ازای هر ساعت یک نمونه می‌گیریم (طی ۲۴ ساعت)
        double sumOccupancy = 0.0;
        int samples = 0;
        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime sampleTime = date.atTime(hour, 0);
            String sampleStr = sampleTime.toString();
            String sql = "SELECT COUNT(*) FROM active_sessions WHERE entry_time <= ? AND (exit_time IS NULL OR exit_time > ?)";
            // توجه: این کوئری باید روی active_sessions و history ترکیبی اجرا شود، به دلیل سادگی از یک روش تقریبی استفاده می‌کنیم
            // در عمل بهتر است یک تابع جداگانه بنویسیم، فعلاً به جای آن فقط occupancy جاری را بر اساس active_sessions برآورد می‌کنیم
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, sampleStr);
                pstmt.setString(2, sampleStr);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int occ = rs.getInt(1);
                    sumOccupancy += (occ * 100.0 / maxCapacity);
                    samples++;
                }
            }
        }
        return (samples == 0) ? 0.0 : (sumOccupancy / samples);
    }


    /**
     * گزارش عملکرد اپراتورها در بازه زمانی معین
     * تعداد ورود/خروج ثبت شده توسط هر اپراتور (با فرض اینکه در جدول parking_history اپراتور ثبت نشده،
     * برای این منظور باید فیلد operator_username را به parking_history اضافه کرد.
     * در اینجا به عنوان نمونه فقط از جداول shifts و active_sessions استفاده می‌کنیم اما برای کامل شدن نیاز به تغییر دیتابیس دارد.
     * فعلاً یک پیاده‌سازی ساده با فرض وجود فیلد operator_username در parking_history ارائه می‌شود.
     */
    public List<OperatorPerformance> getOperatorPerformance(LocalDate from, LocalDate to) throws SQLException {
        List<OperatorPerformance> list = new ArrayList<>();
        String sql = """ 
                SELECT ph.operator_username, COUNT(*) AS transactions
                FROM parking_history ph 
                JOIN users u ON ph.operator_username = u.username 
                WHERE u.role = 'OPERATOR'
                AND date(ph.exit_time) BETWEEN ? AND ?  
                GROUP BY ph.operator_username
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, from.toString());
            pstmt.setString(2, to.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new OperatorPerformance(rs.getString("operator_username"), rs.getInt("transactions")));
            }
        }
        return list;
    }
}


