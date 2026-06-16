package com.parking.manager.database;

import com.parking.manager.model.*;
import com.parking.manager.util.PasswordHasher;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * مدیریت تمام عملیات پایگاه داده SQLite
 * شامل ایجاد جداول، درج، به‌روزرسانی، حذف و جستجو
 */
public class DatabaseManager {

    // فایل دیتابیس در ریشه پروژه (هم سطح pom.xml)
    private static final String DB_URL = "jdbc:sqlite:parking.db";

    // ----------------------------------------------------------------------
    // اتصال به دیتابیس
    // ----------------------------------------------------------------------
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ----------------------------------------------------------------------
    // مقداردهی اولیه (ایجاد جداول و داده‌های پیش‌فرض)
    // ----------------------------------------------------------------------
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. جدول تعرفه‌ها
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tariffs (
                    vehicle_type TEXT PRIMARY KEY,
                    first_hour_cost INTEGER NOT NULL,
                    additional_hour_cost INTEGER NOT NULL,
                    daily_max_cost INTEGER NOT NULL
                )
            """);

            // 2. جدول نشست‌های فعال (خودروهای داخل پارکینگ)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS active_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plate_number TEXT NOT NULL,
                    vehicle_type TEXT NOT NULL,
                    entry_time TEXT NOT NULL
                )
            """);

            // 3. جدول تاریخچه تردد و رسیدها
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS parking_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plate_number TEXT NOT NULL,
                    vehicle_type TEXT NOT NULL,
                    entry_time TEXT NOT NULL,
                    exit_time TEXT NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    total_cost INTEGER NOT NULL,
                    receipt_id TEXT NOT NULL UNIQUE,
                    operator_username TEXT
                )
            """);

            // 4. جدول کاربران با رمز هش شده
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    username TEXT PRIMARY KEY,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL,
                    full_name TEXT,
                    is_archived INTEGER DEFAULT 0
                )
            """);

            // 5. جدول مکان‌های پارکینگ (جای پارک)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS parking_spots (
                    spot_number TEXT PRIMARY KEY,
                    is_occupied INTEGER DEFAULT 0,
                    current_plate_number TEXT,
                    occupied_since TEXT
                )
            """);

            // 6. جدول شیفت‌های کاری اپراتورها
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS shifts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    operator_username TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT,
                    is_active INTEGER DEFAULT 1,
                    FOREIGN KEY(operator_username) REFERENCES users(username)
                )
            """);

            // 7. جدول آمار روزانه (برای گزارش‌گیری سریع)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS daily_stats (
                    date TEXT PRIMARY KEY,
                    total_entries INTEGER DEFAULT 0,
                    total_exits INTEGER DEFAULT 0,
                    total_income INTEGER DEFAULT 0,
                    avg_occupancy_rate REAL DEFAULT 0.0
                )
            """);

            // ---------------- داده‌های پیش‌فرض ----------------
            insertDefaultTariffs();
            initializeParkingSpots();
            insertDefaultUser();  // یک کاربر مالک پیش‌فرض

        } catch (SQLException e) {
            System.err.println("خطا در مقداردهی اولیه دیتابیس: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // تعرفه‌های پیش‌فرض (خودرو و موتور)
    private static void insertDefaultTariffs() {
        String check = "SELECT COUNT(*) FROM tariffs";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(check);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO tariffs (vehicle_type, first_hour_cost, additional_hour_cost, daily_max_cost) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                    pstmt.setString(1, "CAR");
                    pstmt.setInt(2, 20000);
                    pstmt.setInt(3, 10000);
                    pstmt.setInt(4, 80000);
                    pstmt.executeUpdate();

                    pstmt.setString(1, "MOTORCYCLE");
                    pstmt.setInt(2, 10000);
                    pstmt.setInt(3, 5000);
                    pstmt.setInt(4, 40000);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ایجاد ۵۰ جای پارک پیش‌فرض (A01 تا A50)
    private static void initializeParkingSpots() {
        String check = "SELECT COUNT(*) FROM parking_spots";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(check);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO parking_spots (spot_number, is_occupied) VALUES (?, 0)";
                try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                    for (int i = 1; i <= 50; i++) {
                        String spot = String.format("A%02d", i);
                        pstmt.setString(1, spot);
                        pstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ایجاد کاربر مالک پیش‌فرض (نام کاربری: owner ، رمز: admin123)
    private static void insertDefaultUser() {
        String check = "SELECT COUNT(*) FROM users WHERE username = 'owner'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(check);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO users (username, password_hash, role, full_name, is_archived) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                    pstmt.setString(1, "owner");
                    pstmt.setString(2, PasswordHasher.hash("admin123"));
                    pstmt.setString(3, Role.OWNER.name());
                    pstmt.setString(4, "مدیر سیستم");
                    pstmt.setInt(5, 0);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // ----------------------------------------------------------------------
    // تبدیل تاریخ و زمان برای ذخیره در SQLite (ISO format)
    // ----------------------------------------------------------------------
    private static String toString(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static LocalDateTime toLocalDateTime(String str) {
        return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // ----------------------------------------------------------------------
    // عملیات مربوط به تعرفه‌ها
    // ----------------------------------------------------------------------
    public static Tariff getTariff(String vehicleType) throws SQLException {
        String sql = "SELECT * FROM tariffs WHERE vehicle_type = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vehicleType);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Tariff(
                        VehicleType.valueOf(rs.getString("vehicle_type")),
                        rs.getDouble("first_hour_cost"),
                        rs.getDouble("additional_hour_cost"),
                        rs.getDouble("daily_max_cost")
                );
            }
        }
        return null;
    }

    public static void updateTariff(VehicleType type, Tariff tariff) throws SQLException {
        String sql = "INSERT OR REPLACE INTO tariffs (vehicle_type, first_hour_cost, additional_hour_cost, daily_max_cost) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type.name());
            pstmt.setDouble(2, tariff.getFirstHourCost());
            pstmt.setDouble(3, tariff.getAdditionalHourCost());
            pstmt.setDouble(4, tariff.getDailyMaxCost());
            pstmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------------
    // عملیات نشست فعال (ورود/خروج)
    // ----------------------------------------------------------------------
    public static void addActiveSession(String plateNumber, VehicleType type, LocalDateTime entryTime) throws SQLException {
        String sql = "INSERT INTO active_sessions (plate_number, vehicle_type, entry_time) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plateNumber);
            pstmt.setString(2, type.name());
            pstmt.setString(3, toString(entryTime));
            pstmt.executeUpdate();
        }
    }

    public static void removeActiveSession(String plateNumber) throws SQLException {
        String sql = "DELETE FROM active_sessions WHERE plate_number = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plateNumber);
            pstmt.executeUpdate();
        }
    }

    public static List<ParkingSession> getAllActiveSessions() throws SQLException {
        List<ParkingSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM active_sessions";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String plate = rs.getString("plate_number");
                VehicleType type = VehicleType.valueOf(rs.getString("vehicle_type"));
                LocalDateTime entry = toLocalDateTime(rs.getString("entry_time"));
                Vehicle vehicle = new Vehicle(plate, type, entry);
                sessions.add(new ParkingSession(vehicle, entry));
            }
        }
        return sessions;
    }
    // ----------------------------------------------------------------------
    // عملیات تاریخچه و رسید
    // ----------------------------------------------------------------------

    public static void addToHistory(ParkingTicket ticket, String operatorUsername) throws SQLException {
        String sql = """
        INSERT INTO parking_history (plate_number, vehicle_type, entry_time, exit_time, duration_minutes, total_cost, receipt_id, operator_username)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ticket.getVehicle().getPlateNumber());
            pstmt.setString(2, ticket.getVehicle().getType().name());
            pstmt.setString(3, toString(ticket.getVehicle().getEntryTime()));
            pstmt.setString(4, toString(ticket.getExitTime()));
            pstmt.setLong(5, ticket.getDurationMinutes());
            pstmt.setDouble(6, ticket.getTotalCost());
            pstmt.setString(7, ticket.getReceiptId());
            pstmt.setString(8, operatorUsername);   // مقدار جدید
            pstmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------------
    // عملیات کاربران (با رمز هش شده)
    // ----------------------------------------------------------------------
    public static User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND is_archived = 0";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        Role.valueOf(rs.getString("role")),
                        rs.getString("full_name"),
                        false
                );
            }
        }
        return null;
    }

    public static List<User> getAllActiveUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE is_archived = 0";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        Role.valueOf(rs.getString("role")),
                        rs.getString("full_name"),
                        false
                ));
            }
        }
        return users;
    }

    public static void createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, full_name, is_archived) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getRole().name());
            pstmt.setString(4, user.getFullName());
            pstmt.setInt(5, user.isArchived() ? 1 : 0);
            pstmt.executeUpdate();
        }
    }

    public static void updateUser(String username, String newFullName, Role newRole, boolean archived) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, role = ?, is_archived = ? WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newFullName);
            pstmt.setString(2, newRole.name());
            pstmt.setInt(3, archived ? 1 : 0);
            pstmt.setString(4, username);
            pstmt.executeUpdate();
        }
    }

    public static void changePassword(String username, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt= conn.prepareStatement(sql)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }

    public static void changeUsername(String oldUsername, String newUsername) throws SQLException {
        String sql = "UPDATE users SET username = ? WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newUsername);
            pstmt.setString(2, oldUsername);
            pstmt.executeUpdate();
        }
    }
    // ----------------------------------------------------------------------
    // عملیات مکان‌های پارکینگ (جای پارک)
    // ----------------------------------------------------------------------
    public static List<ParkingSpot> getAllParkingSpots() throws SQLException {
        List<ParkingSpot> spots = new ArrayList<>();
        String sql = "SELECT * FROM parking_spots ORDER BY spot_number";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                spots.add(new ParkingSpot(
                        rs.getString("spot_number"),
                        rs.getInt("is_occupied") == 1,
                        rs.getString("current_plate_number"),
                        rs.getString("occupied_since")
                ));
            }
        }
        return spots;
    }

    public static void assignSpotToVehicle(String plateNumber, String spotNumber) throws SQLException {
        String sql = "UPDATE parking_spots SET is_occupied = 1, current_plate_number = ?, occupied_since = datetime('now') WHERE spot_number = ? AND is_occupied = 0";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plateNumber);
            pstmt.setString(2, spotNumber);
            if (pstmt.executeUpdate() == 0) {
                throw new SQLException("جای پارک انتخاب شده پر است یا نامعتبر");
            }
        }
    }

    public static void freeSpotByPlate(String plateNumber) throws SQLException {
        String sql = "UPDATE parking_spots SET is_occupied = 0, current_plate_number = NULL, occupied_since = NULL WHERE current_plate_number = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plateNumber);
            pstmt.executeUpdate();
        }
    }

    // ----------------------------------------------------------------------
    // عملیات شیفت‌های کاری
    // ----------------------------------------------------------------------
    public static void startShift(String operatorUsername) throws SQLException {
        // ابتدا شیفت باز قبلی را می‌بندیم
        endShift(operatorUsername);
        String sql = "INSERT INTO shifts (operator_username, start_time, is_active) VALUES (?, datetime('now'), 1)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, operatorUsername);
            pstmt.executeUpdate();
        }
    }

    public static void endShift(String operatorUsername) throws SQLException {
        String sql = "UPDATE shifts SET end_time = datetime('now'), is_active = 0 WHERE operator_username = ? AND is_active = 1";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, operatorUsername);
            pstmt.executeUpdate();
        }
    }

    public static List<Shift> getOperatorShifts(String operatorUsername, LocalDate from, LocalDate to) throws SQLException {
        List<Shift> shifts = new ArrayList<>();
        String sql = """
            SELECT * FROM shifts 
            WHERE operator_username = ? 
            AND date(start_time) BETWEEN ? AND ?
            ORDER BY start_time DESC
        """;
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, operatorUsername);
            pstmt.setString(2, from.toString());
            pstmt.setString(3, to.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                shifts.add(new Shift(
                        rs.getInt("id"),
                        rs.getString("operator_username"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        rs.getInt("is_active") == 1
                ));
            }
        }
        return shifts;
    }

    // ----------------------------------------------------------------------
    // متدهای کمکی گزارش‌گیری مستقیم از parking_history + active_sessions
    // ----------------------------------------------------------------------
    public static List<ParkingTicket> getHistoryByDateRange(LocalDate from, LocalDate to) throws SQLException {
        List<ParkingTicket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM parking_history WHERE date(entry_time) BETWEEN ? AND ? ORDER BY entry_time";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, from.toString());
            pstmt.setString(2, to.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Vehicle vehicle = new Vehicle(rs.getString("plate_number"), VehicleType.valueOf(rs.getString("vehicle_type")));
                vehicle.setEntryTime(LocalDateTime.parse(rs.getString("entry_time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                ParkingTicket ticket = new ParkingTicket(
                        vehicle,
                        LocalDateTime.parse(rs.getString("exit_time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        rs.getLong("duration_minutes"),
                        rs.getDouble("total_cost"),
                        rs.getString("receipt_id")
                );
                tickets.add(ticket);
            }
        }
        return tickets;
    }

    public static int getActiveEntriesCountForDate(LocalDate date) throws SQLException {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        String sql = "SELECT COUNT(*) FROM active_sessions WHERE entry_time >= ? AND entry_time < ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(2, end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }

    // ----------------------------------------------------------------------
    // متدهای لازم برای ظرفیت پارکینگ
    // ----------------------------------------------------------------------
    // گرفتن لیست بلوک‌های موجود (حرف اول spot_number)
    public static List<String> getDistinctBlocks() throws SQLException {
        List<String> blocks = new ArrayList<>();
        String sql = "SELECT DISTINCT SUBSTR(spot_number, 1, 1) as block FROM parking_spots ORDER BY block";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                blocks.add(rs.getString("block"));
            }
        }
        return blocks;
    }

    // گرفتن تعداد جایگاه‌های یک بلوک
    public static int getSpotCountForBlock(String blockLetter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM parking_spots WHERE spot_number LIKE ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, blockLetter + "%");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }

    // اضافه کردن بلوک جدید
    public static void addBlock(String blockLetter, int spotCount) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM parking_spots WHERE spot_number LIKE ?";
        try (Connection conn = getConnection(); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, blockLetter + "%");
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new SQLException("بلوک " + blockLetter + " قبلاً وجود دارد!");
            }
        }
        String insertSql = "INSERT INTO parking_spots (spot_number, is_occupied) VALUES (?, 0)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (int i = 1; i <= spotCount; i++) {
                String spot = String.format("%s%02d", blockLetter, i);
                pstmt.setString(1, spot);
                pstmt.executeUpdate();
            }
        }
    }

    // حذف یک بلوک (فقط در صورتی که همه جایگاه‌ها خالی باشند)
    public static void removeBlock(String blockLetter) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM parking_spots WHERE spot_number LIKE ? AND is_occupied = 1";
        try (Connection conn = getConnection(); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, blockLetter + "%");
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new SQLException("بلوک " + blockLetter + " دارای جایگاه اشغال شده است! ابتدا خودروها را خارج کنید.");
            }
        }
        String deleteSql = "DELETE FROM parking_spots WHERE spot_number LIKE ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setString(1, blockLetter + "%");
            pstmt.executeUpdate();
        }
    }

    // تغییر تعداد جایگاه‌های یک بلوک (افزایش یا کاهش)
    public static void resizeBlock(String blockLetter, int newTotalSpots) throws SQLException {
        int currentCount = getSpotCountForBlock(blockLetter);
        if (currentCount == newTotalSpots) return;

        if (newTotalSpots > currentCount) {
            // افزایش: جایگاه‌های جدید اضافه کن
            String insertSql = "INSERT INTO parking_spots (spot_number, is_occupied) VALUES (?, 0)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (int i = currentCount + 1; i <= newTotalSpots; i++) {
                    String spot = String.format("%s%02d", blockLetter, i);
                    pstmt.setString(1, spot);
                    pstmt.executeUpdate();
                }
            }
        } else {
            // کاهش: جایگاه‌های اضافی را فقط در صورتی حذف کن که خالی باشند
            String checkSql = "SELECT COUNT(*) FROM parking_spots WHERE spot_number LIKE ? AND is_occupied = 1 AND CAST(SUBSTR(spot_number, 2) AS INTEGER) > ?";
            try (Connection conn = getConnection(); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, blockLetter + "%");
                checkStmt.setInt(2, newTotalSpots);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("جایگاه‌های حذفی باید خالی باشند. ابتدا خودروهای داخل آن جایگاه‌ها را خارج کنید.");
                }
            }
            String deleteSql = "DELETE FROM parking_spots WHERE spot_number LIKE ? AND CAST(SUBSTR(spot_number, 2) AS INTEGER) > ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setString(1, blockLetter + "%");
                pstmt.setInt(2, newTotalSpots);
                pstmt.executeUpdate();
            }
        }
    }


}
