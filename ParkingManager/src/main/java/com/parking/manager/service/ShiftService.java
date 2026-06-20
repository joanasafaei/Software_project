package com.parking.manager.service;

import com.parking.manager.database.DatabaseManager;
import com.parking.manager.model.Role;
import com.parking.manager.model.Shift;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * سرویس مدیریت شیفت‌های کاری اپراتورها
 */
public class ShiftService {

    /**
     * شروع شیفت برای اپراتور (اگر شیفت باز قبلی باشد، ابتدا آن را می‌بندد)
     */
    public void startShift(String operatorUsername, Role requesterRole) throws SQLException {
        // فقط اپراتور خودش یا ادمین/مالک می‌توانند شروع کنند
        if (requesterRole == Role.OPERATOR) {
            // اپراتور فقط می‌تواند برای خودش شیفت باز کند
            // (در کنترلر باید بررسی شود که درخواست‌دهنده همان operatorUsername باشد)
        } else if (requesterRole != Role.OWNER && requesterRole != Role.ADMIN) {
            throw new SecurityException("دسترسی غیرمجاز برای مدیریت شیفت.");
        }
        DatabaseManager.startShift(operatorUsername);
    }

    /**
     * پایان شیفت جاری اپراتور
     */
    public void endShift(String operatorUsername, Role requesterRole) throws SQLException {
        if (requesterRole == Role.OPERATOR) {
            // اپراتور فقط می‌تواند شیفت خودش را ببندد (در کنترلر بررسی می‌شود)
        } else if (requesterRole != Role.OWNER && requesterRole != Role.ADMIN) {
            throw new SecurityException("دسترسی غیرمجاز برای مدیریت شیفت.");
        }
        DatabaseManager.endShift(operatorUsername);
    }

    /**
     * دریافت شیفت‌های یک اپراتور در بازه زمانی
     */
    public List<Shift> getShiftsForOperator(String operatorUsername, LocalDate from, LocalDate to, Role requesterRole) throws SQLException {
        if (requesterRole == Role.OPERATOR && !requesterRole.name().equals(operatorUsername)) {
            // اپراتور فقط مجاز به دیدن شیفت‌های خودش است
            throw new SecurityException("اپراتور فقط می‌تواند شیفت‌های خود را ببیند.");
        }
        return DatabaseManager.getOperatorShifts(operatorUsername, from, to);
    }

    /**
     * (اختیاری) گزارش خلاصه شیفت برای یک اپراتور در یک روز
     */
    public ShiftSummary getShiftSummary(String operatorUsername, LocalDate date) throws SQLException {
        List<Shift> shifts = DatabaseManager.getOperatorShifts(operatorUsername, date, date);
        if (shifts.isEmpty()) return null;
        Shift shift = shifts.get(0); // فرض می‌کنیم در یک روز فقط یک شیفت فعال/بسته وجود دارد
        // محاسبه تعداد ترددهای ثبت شده توسط این اپراتور در این شیفت
        // نیاز به join با parking_history دارد – فعلاً ساده می‌پذیریم
        return new ShiftSummary(shift, 0, 0, 0); // می‌توانید تکمیل کنید
    }
}

class ShiftSummary {
    public Shift shift;
    public int entriesCount, exitsCount, totalIncome;
    public ShiftSummary(Shift s, int entries, int exits, int income) {
        this.shift = s; this.entriesCount = entries; this.exitsCount = exits; this.totalIncome = income;
    }
}
