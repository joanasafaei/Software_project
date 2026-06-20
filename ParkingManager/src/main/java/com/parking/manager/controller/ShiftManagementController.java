package com.parking.manager.controller;

import com.parking.manager.model.*;
import com.parking.manager.service.AuthService;
import com.parking.manager.service.ShiftService;
import com.parking.manager.util.PersianDatePickerUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * کنترلر مدیریت شیفت‌های کاری اپراتورها
 * اپراتور فقط می‌تواند شیفت خود را ببیند و شروع/پایان دهد، ادمین/مالک همه را می‌بینند
 */
public class ShiftManagementController {
    private User currentUser;
    private AuthService authService = new AuthService();
    private ShiftService shiftService = new ShiftService();
    private BorderPane root;
    private TableView<Shift> shiftTable;
    private ComboBox<String> operatorCombo; // برای انتخاب اپراتور (فقط ادمین/مالک)
    private DatePicker fromDate, toDate;

    public ShiftManagementController(User currentUser) throws SQLException {
        this.currentUser = currentUser;
        buildUI();
    }

    private void buildUI() throws SQLException {
        root = new BorderPane();
        root.setPadding(new Insets(10));

        Label title = new Label("مدیریت شیفت‌های کاری");
        title.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;");

        // بخش فیلتر
        fromDate = new DatePicker(LocalDate.now().minusDays(7));
        toDate = new DatePicker(LocalDate.now());
        PersianDatePickerUtil.setupPersianDatePicker(fromDate);
        PersianDatePickerUtil.setupPersianDatePicker(toDate);
        fromDate.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        toDate.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        Button btnFilter = new Button("فیلتر");

        HBox filterBox = new HBox(10, toDate, new Label("تا:"), fromDate, new Label("از:"), btnFilter);
        filterBox.setPadding(new Insets(10));
        filterBox.setAlignment(Pos.TOP_RIGHT);

        // انتخاب اپراتور برای ادمین/مالک
        if (currentUser.getRole() != Role.OPERATOR) {
            operatorCombo = new ComboBox<>();
            operatorCombo.setPromptText("انتخاب اپراتور");
            filterBox.getChildren().addAll(operatorCombo, new Label("اپراتور:"));
            // بارگذاری لیست اپراتورها – ساده: می‌توان از AuthService گرفت
            // دریافت لیست کاربران فعال
            List<User> users = authService.listActiveUsers(currentUser.getRole());
            // فیلتر کردن فقط اپراتورها و گرفتن نام کاربری
            List<String> operatorUsernames = users.stream()
                    .filter(u -> u.getRole() == Role.OPERATOR)
                    .map(User::getUsername)
                    .collect(Collectors.toList());
            // اضافه کردن به کامبوباکس
            operatorCombo.getItems().setAll(operatorUsernames);
        }

        shiftTable = new TableView<>();
        TableColumn<Shift, String> colOp = new TableColumn<>("اپراتور");
        colOp.setMinWidth(150);
        colOp.setStyle("-fx-alignment: center");
        colOp.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getOperatorUsername()));
        TableColumn<Shift, String> colStart = new TableColumn<>("شروع شیفت");
        colStart.setMinWidth(220);
        colStart.setStyle("-fx-alignment: center");
        colStart.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStartTime()));
        TableColumn<Shift, String> colEnd = new TableColumn<>("پایان شیفت");
        colEnd.setMinWidth(220);
        colEnd.setStyle("-fx-alignment: center");
        colEnd.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getEndTime() == null ? "فعال" : cell.getValue().getEndTime()));
        shiftTable.getColumns().addAll(colOp, colStart, colEnd);
        shiftTable.setPrefHeight(380);
        shiftTable.setStyle("-fx-text-fill: Black;");
        shiftTable.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        Button btnStart = new Button("شروع شیفت من");
        Button btnEnd = new Button("پایان شیفت من");
        HBox buttons = new HBox(15, btnStart, btnEnd);
        buttons.setAlignment(Pos.CENTER);

        if (currentUser.getRole() == Role.OPERATOR) {
            btnStart.setOnAction(e -> startMyShift());
            btnEnd.setOnAction(e -> endMyShift());
        } else {
            // برای ادمین/مالک می‌توان دکمه‌های اضافه برای شروع/پایان شیفت سایرین قرار داد
            btnStart.setDisable(true);
            btnEnd.setDisable(true);
        }

        VBox vbox = new VBox(10, title, filterBox, shiftTable, buttons);
        vbox.setPadding(new Insets(10));
        vbox.setAlignment(Pos.TOP_RIGHT);
        root.setCenter(vbox);

        btnFilter.setOnAction(e -> loadShifts());

        //کلیدهای میانبر منو شیفت
        root.setOnKeyPressed(e -> {
            switch (e.getCode()){
                case F1:
                    btnStart.fire();
                    e.consume();
                    break;
                case F2:
                    btnEnd.fire();
                    e.consume();
                    break;
                default:
            }
            e.consume();
        });
    }

    private void loadShifts() {
        try {
            LocalDate from = fromDate.getValue();
            LocalDate to = toDate.getValue();
            if (from == null || to == null) return;
            String targetOperator;
            if (currentUser.getRole() == Role.OPERATOR) {
                targetOperator = currentUser.getUsername();
            } else {
                targetOperator = operatorCombo.getValue();
                if (targetOperator == null) {
                    showAlert("لطفاً اپراتور را انتخاب کنید.");
                    return;
                }
            }
            List<Shift> shifts = shiftService.getShiftsForOperator(targetOperator, from, to, currentUser.getRole());
            shiftTable.setItems(FXCollections.observableArrayList(shifts));
        } catch (SQLException | SecurityException e) {
            showAlert("خطا: " + e.getMessage());
        }
    }

    private void startMyShift() {
        try {
            shiftService.startShift(currentUser.getUsername(), currentUser.getRole());
            showAlert("شیفت شما شروع شد.");
            loadShifts();
        } catch (Exception e) {
            showAlert("خطا: " + e.getMessage());
        }
    }

    private void endMyShift() {
        try {
            shiftService.endShift(currentUser.getUsername(), currentUser.getRole());
            showAlert("شیفت شما پایان یافت.");
            loadShifts();
        } catch (Exception e) {
            showAlert("خطا: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public Parent getRoot() { return root; }
}
