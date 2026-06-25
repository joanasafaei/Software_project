package com.parking.manager.util;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.PersianCalendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * ابزار تبدیل تاریخ میلادی به شمسی با استفاده از ICU4J
 */
public class PersianDateUtil {

    // فرمت پیش‌فرض تاریخ و زمان شمسی
    private static final String DEFAULT_DATETIME_PATTERN = "yyyy/MM/dd HH:mm:ss";
    private static final String DEFAULT_DATE_PATTERN = "yyyy/MM/dd";

    // برای کارایی بهتر، یک نمونه از SimpleDateFormat با PersianCalendar نگه می‌داریم
    private static final SimpleDateFormat DATE_TIME_FORMATTER;
    private static final SimpleDateFormat DATE_FORMATTER;

    static {
        // ایجاد PersianCalendar برای تنظیم تقویم
        Calendar persianCalendar = new PersianCalendar();

        DATE_TIME_FORMATTER = new SimpleDateFormat(DEFAULT_DATETIME_PATTERN);
        DATE_TIME_FORMATTER.setCalendar(persianCalendar);

        DATE_FORMATTER = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
        DATE_FORMATTER.setCalendar(persianCalendar);
    }

    /**
     * دریافت تاریخ و زمان جاری سیستم به صورت شمسی
     * @return رشته شمسی با فرمت yyyy/MM/dd HH:mm:ss
     */
    public static String getCurrentPersianDateTime() {
        return DATE_TIME_FORMATTER.format(new Date());
    }

    /**
     * دریافت تاریخ جاری سیستم به صورت شمسی (بدون ساعت)
     * @return رشته شمسی با فرمت yyyy/MM/dd
     */
    public static String getCurrentPersianDate() {
        return DATE_FORMATTER.format(new Date());
    }

    /**
     * تبدیل LocalDateTime به رشته شمسی با فرمت کامل
     * @param dateTime تاریخ میلادی
     * @return رشته شمسی با فرمت yyyy/MM/dd HH:mm:ss
     */
    public static String toPersianDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        return DATE_TIME_FORMATTER.format(date);
    }

    /**
     * تبدیل LocalDateTime به رشته شمسی (فقط تاریخ)
     * @param dateTime تاریخ میلادی
     * @return رشته شمسی با فرمت yyyy/MM/dd
     */
    public static String toPersianDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        return DATE_FORMATTER.format(date);
    }

    /**
     * تبدیل مستقیم شیء Date به رشته شمسی با فرمت کامل
     * @param date تاریخ میلادی
     * @return رشته شمسی
     */
    public static String toPersianDateTime(Date date) {
        if (date == null) return "";
        return DATE_TIME_FORMATTER.format(date);
    }

    /**
     * تبدیل مستقیم شیء Date به رشته شمسی (فقط تاریخ)
     * @param date تاریخ میلادی
     * @return رشته شمسی
     */
    public static String toPersianDate(Date date) {
        if (date == null) return "";
        return DATE_FORMATTER.format(date);
    }

    public static LocalDate persianYearMonthToGregorianStart(int persianYear, int persianMonth) {
        com.ibm.icu.util.PersianCalendar pc = new com.ibm.icu.util.PersianCalendar();
        pc.clear();
        pc.set(persianYear, persianMonth - 1, 1);
        return LocalDate.ofInstant(pc.getTime().toInstant(), ZoneOffset.UTC);
    }

    public static LocalDate persianYearMonthToGregorianEnd(int persianYear, int persianMonth) {
        com.ibm.icu.util.PersianCalendar pc = new com.ibm.icu.util.PersianCalendar();
        pc.clear();
        pc.set(persianYear, persianMonth - 1, 1);
        int lastDay = pc.getActualMaximum(com.ibm.icu.util.Calendar.DAY_OF_MONTH);
        pc.set(persianYear, persianMonth - 1, lastDay);
        return LocalDate.ofInstant(pc.getTime().toInstant(), ZoneOffset.UTC);
    }
}