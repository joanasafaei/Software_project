package com.parking.manager.controller;

import com.parking.manager.model.DailyStat;
import com.parking.manager.model.User;
import com.parking.manager.service.OperatorPerformance;
import com.parking.manager.service.ReportService;
import com.parking.manager.util.PersianDatePickerUtil;
import com.parking.manager.util.PersianDateUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * کنترلر گزارشات روزانه، هفتگی، ماهانه از درآمد، تردد، اشغال و عملکرد اپراتورها
 */
public class ReportController {
    private User currentUser;
    private ReportService reportService = new ReportService();
    private BorderPane root;
    private TabPane tabPane;

    public ReportController(User currentUser) {
        this.currentUser = currentUser;
        buildUI();
    }

    private void buildUI() {
        root = new BorderPane();
        root.setPadding(new Insets(10));

        tabPane = new TabPane();
        Tab dailyTab = new Tab("گزارش روزانه");
        Tab weeklyTab = new Tab("گزارش هفتگی");
        Tab monthlyTab = new Tab("گزارش ماهانه");
        Tab customTab = new Tab("گزارش بازه دلخواه");
        Tab operatorTab = new Tab("عملکرد اپراتورها");

        tabPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        dailyTab.setContent(createDailyPanel());
        weeklyTab.setContent(createWeeklyPanel());
        monthlyTab.setContent(createMonthlyPanel());
        customTab.setContent(createCustomRangePanel());
        operatorTab.setContent(createOperatorPanel());

        tabPane.getTabs().addAll(dailyTab, weeklyTab, monthlyTab, customTab, operatorTab);

        root.setCenter(tabPane);
    }

    // ========================== گزارش روزانه ==========================
    private VBox createDailyPanel() {
        DatePicker datePicker = new DatePicker(LocalDate.now());
        PersianDatePickerUtil.setupPersianDatePicker(datePicker);
        Button btnShow = new Button("نمایش");
        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefHeight(400);

        btnShow.setOnAction(e -> {
            LocalDate gregorianDate = datePicker.getValue();
            if (gregorianDate == null) {
                reportArea.setText("تاریخی انتخاب کنید.");
                reportArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");
                return;
            }
            try {
                DailyStat stat = reportService.getDailyStat(gregorianDate);
                if (stat != null) {
                    String shamsiDate = PersianDatePickerUtil.toPersianString(gregorianDate);
                    String text = String.format(
                            "گزارش روزانه %s:\nورود: %d\nخروج: %d\nدرآمد: %,d ریال\nمیانگین اشغال: %.2f%%",
                            shamsiDate, stat.getTotalEntries(), stat.getTotalExits(),
                            stat.getTotalIncome(), stat.getAvgOccupancyRate()
                    );
                    reportArea.setText(text);
                    reportArea.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                } else {
                    reportArea.setText("آماری برای این روز یافت نشد.");
                }
                reportArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");

            } catch (Exception ex) {
                reportArea.setText("خطا: " + ex.getMessage());
            }
        });

        VBox vbox = new VBox(10, new Label("انتخاب تاریخ:"), datePicker, btnShow, reportArea);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    // ========================== گزارش هفتگی ==========================
    private VBox createWeeklyPanel() {
        // فقط یک DatePicker برای تاریخ پایان
        DatePicker endDatePicker = new DatePicker(LocalDate.now());
        PersianDatePickerUtil.setupPersianDatePicker(endDatePicker);
        endDatePicker.setPromptText("آخرین روز هفته مورد نظر");

        Button btnShow = new Button("نمایش");
        TextArea weeklyArea = new TextArea();
        weeklyArea.setEditable(false);
        weeklyArea.setPrefHeight(400);

        btnShow.setOnAction(e -> {
            LocalDate endDate = endDatePicker.getValue();
            if (endDate == null) {
                weeklyArea.setText("تاریخ پایان هفته را انتخاب کنید.");
                weeklyArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");
                return;
            }
            // محاسبه تاریخ شروع (7 روز قبل)
            LocalDate startDate = endDate.minusDays(6); // 7 روز شامل start تا end

            try {
                //  با حلقه روزها را جمع می‌زنیم:
                int totalEntries = 0, totalExits = 0, totalIncome = 0;
                double totalOccupancy = 0.0;
                int days = 0;
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    DailyStat stat = reportService.getDailyStat(d);
                    if (stat != null) {
                        totalEntries += stat.getTotalEntries();
                        totalExits += stat.getTotalExits();
                        totalIncome += stat.getTotalIncome();
                        totalOccupancy += stat.getAvgOccupancyRate();
                        days++;
                    }
                }
                double avgOccupancy = (days == 0) ? 0.0 : (totalOccupancy / days);

                String shamsiStart = PersianDatePickerUtil.toPersianString(startDate);
                String shamsiEnd = PersianDatePickerUtil.toPersianString(endDate);
                String text = String.format(
                        "گزارش هفتگی از %s تا %s:\nکل ورود: %d\nکل خروج: %d\nکل درآمد: %,d ریال\nمیانگین اشغال: %.2f%%",
                        shamsiStart, shamsiEnd,
                        totalEntries, totalExits, totalIncome, avgOccupancy
                );
                weeklyArea.setText(text);
                weeklyArea.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                weeklyArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");

            } catch (Exception ex) {
                weeklyArea.setText("خطا: " + ex.getMessage());
            }
        });

        VBox vbox = new VBox(10, new Label("انتخاب تاریخ پایان هفته:"), endDatePicker, btnShow, weeklyArea);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    // ========================== گزارش ماهانه ==========================
    private VBox createMonthlyPanel() {
        // انتخاب سال و ماه شمسی با کمک ComboBox
        // ابتدا سال جاری شمسی را با PersianCalendar از icu4j بدست می‌آوریم
        com.ibm.icu.util.PersianCalendar now = new com.ibm.icu.util.PersianCalendar();
        int currentPersianYear = now.get(com.ibm.icu.util.Calendar.YEAR);

        ComboBox<Integer> yearCombo = new ComboBox<>();
        for (int y = currentPersianYear - 5; y <= currentPersianYear + 5; y++) {
            yearCombo.getItems().add(y);
        }
        yearCombo.setValue(currentPersianYear);

        ComboBox<String> monthCombo = new ComboBox<>();
        String[] persianMonths = {"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"};
        monthCombo.getItems().addAll(persianMonths);
        monthCombo.setValue(persianMonths[0]);

        Button btnShow = new Button("نمایش");
        TextArea monthlyArea = new TextArea();
        monthlyArea.setEditable(false);
        monthlyArea.setPrefHeight(400);

        btnShow.setOnAction(e -> {
            int persianYear = yearCombo.getValue();
            int persianMonthIndex = monthCombo.getSelectionModel().getSelectedIndex();
            if (persianMonthIndex < 0) return;
            int persianMonth = persianMonthIndex + 1;

            // تبدیل به بازه میلادی با استفاده از PersianCalendar (icu4j)
            LocalDate start = PersianDateUtil.persianYearMonthToGregorianStart(persianYear, persianMonth);
            LocalDate end = PersianDateUtil.persianYearMonthToGregorianEnd(persianYear, persianMonth);
            if (start == null || end == null) {
                monthlyArea.setText("خطا در تبدیل تاریخ");
                monthlyArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");
                return;
            }

            try {
                int totalEntries = 0, totalExits = 0, totalIncome = 0;
                double totalOccupancy = 0.0;
                int days = 0;
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    DailyStat stat = reportService.getDailyStat(d);
                    if (stat != null) {
                        totalEntries += stat.getTotalEntries();
                        totalExits += stat.getTotalExits();
                        totalIncome += stat.getTotalIncome();
                        totalOccupancy += stat.getAvgOccupancyRate();
                        days++;
                    }
                }
                double avgOccupancy = (days == 0) ? 0.0 : (totalOccupancy / days);

                String text = String.format(
                        "گزارش ماهانه %s %d:\nکل ورود: %d\nکل خروج: %d\nکل درآمد: %,d ریال\nمیانگین اشغال: %.2f%%",
                        persianMonths[persianMonthIndex], persianYear,totalEntries, totalExits, totalIncome, avgOccupancy
                );
                monthlyArea.setText(text);
                monthlyArea.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                monthlyArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");

            } catch (Exception ex) {
                monthlyArea.setText("خطا: " + ex.getMessage());
            }
        });

        HBox selectionBox = new HBox(10, new Label("سال شمسی:"), yearCombo, new Label("ماه:"), monthCombo);
        VBox vbox = new VBox(10, selectionBox, btnShow, monthlyArea);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    // ========================== گزارش بازه دلخواه ==========================
    private VBox createCustomRangePanel() {
        DatePicker startPicker = new DatePicker();
        DatePicker endPicker = new DatePicker();
        PersianDatePickerUtil.setupPersianDatePicker(startPicker);
        PersianDatePickerUtil.setupPersianDatePicker(endPicker);
        Button btnShow = new Button("نمایش");
        TextArea rangeArea = new TextArea();
        rangeArea.setEditable(false);
        rangeArea.setPrefHeight(400);

        btnShow.setOnAction(e -> {
            LocalDate start = startPicker.getValue();
            LocalDate end = endPicker.getValue();
            if (start == null || end == null) {
                rangeArea.setText("هر دو تاریخ را انتخاب کنید.");
                rangeArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");
                return;
            }
            if (start.isAfter(end)) {
                rangeArea.setText("تاریخ شروع باید قبل از پایان باشد.");
                rangeArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");
                return;
            }
            try {
                int totalEntries = 0, totalExits = 0, totalIncome = 0;
                double totalOccupancy = 0.0;
                int days = 0;
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    DailyStat stat = reportService.getDailyStat(d);
                    if (stat != null) {
                        totalEntries += stat.getTotalEntries();
                        totalExits += stat.getTotalExits();
                        totalIncome += stat.getTotalIncome();
                        totalOccupancy += stat.getAvgOccupancyRate();
                        days++;
                    }
                }
                double avgOccupancy = (days == 0) ? 0.0 : (totalOccupancy / days);
                String shamsiStart = PersianDatePickerUtil.toPersianString(start);
                String shamsiEnd = PersianDatePickerUtil.toPersianString(end);
                String text = String.format(
                        "گزارش بازه %s تا %s:\nکل ورود: %d\nکل خروج: %d\nکل درآمد: %,d ریال\nمیانگین اشغال: %.2f%%",
                        shamsiStart, shamsiEnd, totalEntries, totalExits, totalIncome, avgOccupancy
                );
                rangeArea.setText(text);
                rangeArea.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                rangeArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");

            } catch (Exception ex) {
                rangeArea.setText("خطا: " + ex.getMessage());
            }
        });

        HBox pickerBox = new HBox(20, new Label("از تاریخ:"), startPicker, new Label("تا تاریخ:"), endPicker);
        VBox vbox = new VBox(10, pickerBox, btnShow, rangeArea);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    // ========================== عملکرد اپراتورها ==========================
    private VBox createOperatorPanel() {
        DatePicker startPicker = new DatePicker();
        DatePicker endPicker = new DatePicker();
        PersianDatePickerUtil.setupPersianDatePicker(startPicker);
        PersianDatePickerUtil.setupPersianDatePicker(endPicker);
        Button btnShow = new Button("نمایش");
        TextArea operatorArea = new TextArea();
        operatorArea.setEditable(false);
        operatorArea.setPrefHeight(400);

        btnShow.setOnAction(e -> {
            LocalDate start = startPicker.getValue();
            LocalDate end = endPicker.getValue();
            if (start == null || end == null) {
                operatorArea.setText("بازه زمانی را انتخاب کنید.");
                operatorArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");
                return;
            }
            if (start.isAfter(end)) {
                operatorArea.setText("تاریخ شروع باید قبل از پایان باشد.");
                operatorArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");
                return;
            }
            try {
                List<OperatorPerformance> list = reportService.getOperatorPerformance(start, end);
                if (list == null || list.isEmpty()) {
                    operatorArea.setText("هیچ تراکنشی برای این بازه یافت نشد.");
                    operatorArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("عملکرد اپراتورها از ")
                        .append(PersianDatePickerUtil.toPersianString(start))
                        .append(" تا ")
                        .append(PersianDatePickerUtil.toPersianString(end))
                        .append(":\n\n");
                for (OperatorPerformance op : list) {
                    sb.append("اپراتور: ").append(op.operatorUsername)
                            .append(" - تعداد تراکنش‌ها: ").append(op.transactionsCount)
                            .append("\n");
                }
                operatorArea.setText(sb.toString());
                operatorArea.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                operatorArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");

            } catch (Exception ex) {
                operatorArea.setText("خطا: " + ex.getMessage());
                operatorArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");

            }
        });

        HBox pickerBox = new HBox(20, new Label("از تاریخ:"), startPicker, new Label("تا تاریخ:"), endPicker);
        VBox vbox = new VBox(10, pickerBox, btnShow, operatorArea);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    public Parent getRoot() { return root; }
}
