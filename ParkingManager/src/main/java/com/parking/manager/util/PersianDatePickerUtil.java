package com.parking.manager.util;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.PersianCalendar;
import com.ibm.icu.util.TimeZone;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Locale;

public class PersianDatePickerUtil {

    private static final SimpleDateFormat PERSIAN_DATE_FORMAT;

    static {
        PersianCalendar calendar = new PersianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        PERSIAN_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd", new Locale("fa", "IR"));
        PERSIAN_DATE_FORMAT.setCalendar(calendar);
        PERSIAN_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // تبدیل میلادی به رشته شمسی (برای نمایش در فیلد)
    public static String toPersianString(LocalDate gregorianDate) {
        if (gregorianDate == null) return "";
        Date date = Date.from(gregorianDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        return PERSIAN_DATE_FORMAT.format(date);
    }

    // تبدیل رشته شمسی به میلادی (برای خواندن از فیلد)
    public static LocalDate fromPersianString(String persianDateStr) {
        if (persianDateStr == null || persianDateStr.trim().isEmpty()) return null;
        try {
            Date date = PERSIAN_DATE_FORMAT.parse(persianDateStr);
            return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // تنظیم DatePicker
    public static void setupPersianDatePicker(DatePicker datePicker) {
        // تبدیل‌گر متن فیلد
        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return toPersianString(date);
            }
            @Override
            public LocalDate fromString(String string) {
                return fromPersianString(string);
            }
        };
        datePicker.setConverter(converter);
        datePicker.setPromptText("yyyy/MM/dd");

        // سفارشی‌سازی سلول‌های روز (اعداد شمسی و رنگ جمعه)
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    int persianDay = getPersianDayOfMonth(item);
                    setText(String.valueOf(persianDay));
                    setStyle(isFriday(item) ? "-fx-text-fill: red;" : "-fx-text-fill: black;");
                }
            }
        });
    }

    private static int getPersianDayOfMonth(LocalDate gregorianDate) {
        PersianCalendar pc = new PersianCalendar(TimeZone.getTimeZone("UTC"));
        pc.setTime(Date.from(gregorianDate.atStartOfDay(ZoneOffset.UTC).toInstant()));
        return pc.get(Calendar.DAY_OF_MONTH);
    }

    private static boolean isFriday(LocalDate gregorianDate) {
        PersianCalendar pc = new PersianCalendar(TimeZone.getTimeZone("UTC"));
        pc.setTime(Date.from(gregorianDate.atStartOfDay(ZoneOffset.UTC).toInstant()));
        return pc.get(Calendar.DAY_OF_WEEK) == 6; // جمعه
    }
}