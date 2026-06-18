package com.parking.manager.controller;

import com.parking.manager.database.DatabaseManager;
import com.parking.manager.model.*;
import com.parking.manager.service.ParkingService;
import com.parking.manager.service.ShiftService;
import com.parking.manager.util.PersianDateUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * کنترلر اصلی برنامه – منوی اصلی و نمایش ظرفیت لحظه‌ای و لیست وسایل داخل
 * دسترسی به بخش‌های مختلف بر اساس نقش کاربر محدود می‌شود
 */
public class MainController {
    private User currentUser;
    private ParkingService parkingService = new ParkingService();
    private ShiftService shiftService = new ShiftService();
    private BorderPane root;
    private ListView<String> activeListView;
    private TextArea receiptArea;
    private Label capacityLabel;

    // بخش‌های مختلف برنامه (که به صورت پنل در مرکز نمایش داده می‌شوند)
    private UserManagementController userManagementController;
    private ShiftManagementController shiftManagementController;
    private ReportController reportController;
    private ProfileController profileController;

    public MainController(User user) {
        this.currentUser = user;
        buildUI();
        refreshActiveList();
        startShiftIfOperator(); // اگر اپراتور است، شیفت را شروع کن
    }

    private void buildUI() {
        root = new BorderPane();
        root.setPadding(new Insets(10));


        // ----- نوار بالایی: عنوان + ظرفیت + خوش‌آمدگویی -----
        Label title = new Label("داشبورد مدیریت پارکینگ");

        capacityLabel = new Label();
        capacityLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #22f122;");
        updateCapacityLabel();

        Label welcomeLabel = new Label("خوش آمدید: " + currentUser.getFullName() + " (" + translateRole(currentUser.getRole()) + ")");
        welcomeLabel.setStyle("-fx-font-size: 14px;");

        HBox topBar = new HBox(20, title, capacityLabel, welcomeLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));
        root.setTop(topBar);

        // ----- بخش مرکزی: لیست وسایل داخل + رسید -----
        activeListView = new ListView<>();
        activeListView.setPrefHeight(500);
        activeListView.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        Label listLabel = new Label("وسایل نقلیه داخل پارکینگ:");
        VBox rightBox = new VBox(10, listLabel, activeListView);
        rightBox.setPadding(new Insets(10));
        rightBox.setPrefWidth(500);
        rightBox.setAlignment(Pos.TOP_RIGHT);

        receiptArea = new TextArea();
        receiptArea.setEditable(false);
        receiptArea.setPromptText("رسید خروج در اینجا نمایش داده می‌شود...");
        receiptArea.setPrefHeight(350);
        receiptArea.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        receiptArea.setStyle("-fx-text-fill: black;-fx-alignment: right;-fx-font-weight: bold;");
        Label receiptLabel = new Label("رسید دیجیتال:");
        Button printBtn = new Button("🖨️ چاپ رسید");
        printBtn.setOnAction(e -> printReceipt());
        VBox leftBox = new VBox(10, receiptLabel, receiptArea, printBtn);
        leftBox.setPadding(new Insets(10));
        leftBox.setPrefWidth(500);
        leftBox.setAlignment(Pos.TOP_RIGHT);
        printBtn.setAlignment(Pos.CENTER);

        HBox centerBox = new HBox(20, leftBox, rightBox);
        centerBox.setAlignment(Pos.TOP_CENTER);
        root.setCenter(centerBox);


        // ----- نوار پایینی: دکمه‌های عملیاتی (بر اساس نقش) -----
        Button btnEntry = new Button("ثبت ورود");
        Button btnExit = new Button("ثبت خروج");
        Button btnRefresh = new Button("بروزرسانی لیست");
        Button btnShowSpots = new Button("مشاهده جایگاه‌ها");
        Button btnUsers = new Button("کاربران");
        Button btnShifts = new Button("شیفت‌ها");
        Button btnTariff = new Button("تعرفه‌ها");
        Button btnReports = new Button("گزارشات");
        Button btnProfile = new Button("پروفایل من");
        Button btnHome = new Button("صفحه اصلی");
        Button btnExitApp = new Button("خروج");

        // محدودیت دسترسی بر اساس نقش
        if (currentUser.getRole() == Role.OPERATOR) {
            btnUsers.setDisable(true);
            btnShifts.setDisable(true);
            btnReports.setDisable(true);
            btnTariff.setDisable(true);
        } else if (currentUser.getRole() == Role.ADMIN) {
            // ادمین می‌تواند همه چیز به جز مدیریت کاربران سطح بالا (که در UserManagementController کنترل می‌شود)
            // در اینجا دکمه مدیریت کاربران را فعال می‌گذاریم ولی در کنترلر آن محدودیت اعمال می‌شود
        } // مالک همه چیز فعال

        HBox buttonBar = new HBox(15, btnEntry, btnExit, btnRefresh, btnShowSpots,
                btnUsers, btnShifts, btnTariff, btnReports, btnProfile, btnHome, btnExitApp);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10));
        //buttonBar.setWrapText(true);
        root.setBottom(buttonBar);

        // رویدادها
        btnEntry.setOnAction(e -> showEntryDialog());
        btnExit.setOnAction(e -> showExitDialog());
        btnHome.setOnAction(e -> showMainView(centerBox));
        btnTariff.setOnAction(e -> showTariffDialog());
        btnRefresh.setOnAction(e -> refreshActiveList());
        btnShowSpots.setOnAction(e -> showParkingSpotsDialog());
        btnUsers.setOnAction(e -> openUserManagement());
        btnShifts.setOnAction(e -> {
            try {
                openShiftManagement();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        btnReports.setOnAction(e -> openReports());
        btnProfile.setOnAction(e -> openProfile());
        btnExitApp.setOnAction(e -> {
            if (currentUser.getRole() == Role.OPERATOR) {
                try {
                    shiftService.endShift(currentUser.getUsername(), currentUser.getRole());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            Platform.exit();
        });


        //کلیدهای میانبر منو اصلی
        root.setOnKeyPressed(e -> {
            switch (e.getCode()){
                case F2:
                    printBtn.fire();
                    break;
                case I:
                    btnEntry.fire();
                    break;
                case E:
                    btnExit.fire();
                    break;
                case A:
                    btnRefresh.fire();
                    break;
                case S:
                    btnShowSpots.fire();
                    break;
                case U:
                    btnUsers.fire();
                    break;
                case O:
                    btnShifts.fire();
                    break;
                case T:
                    btnTariff.fire();
                    break;
                case R:
                    btnReports.fire();
                    break;
                case P:
                    btnProfile.fire();
                    break;
                case H:
                    btnHome.fire();
                    break;
                case ESCAPE:
                    btnExitApp.fire();
                    break;
                default:
            }
            e.consume();
        });
    }
    private void showMainView(HBox centerBox) {
        if (centerBox != null) {
            root.setCenter(centerBox);
            refreshActiveList();  //  لیست را هم به روز می کند
        }
    }

    private void showTariffDialog() {
        // فقط مالک یا ادمین مجازند
        if (currentUser.getRole() != Role.OWNER && currentUser.getRole() != Role.ADMIN) {
            showAlert("دسترسی غیرمجاز! فقط مالک یا ادمین می‌توانند تعرفه را ویرایش کنند.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("ویرایش تعرفه‌ها");
        dialog.setHeaderText("تعرفه جدید را وارد کنید (مقادیر به ریال)");

        ComboBox<VehicleType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(VehicleType.values());
        typeCombo.setValue(VehicleType.CAR);

        TextField firstHourField = new TextField();
        TextField additionalHourField = new TextField();
        TextField dailyMaxField = new TextField();


        // بارگذاری تعرفه فعلی
        try {
            Tariff currentTariff = parkingService.getTariff(typeCombo.getValue());
            if (currentTariff != null) {
                firstHourField.setText(String.valueOf((int) currentTariff.getFirstHourCost()));
                additionalHourField.setText(String.valueOf((int) currentTariff.getAdditionalHourCost()));
                dailyMaxField.setText(String.valueOf((int) currentTariff.getDailyMaxCost()));
            }
        } catch (SQLException ex) {
            showAlert("خطا در دریافت تعرفه: " + ex.getMessage());
        }

        typeCombo.setOnAction(e -> {
            try {
                Tariff t = parkingService.getTariff(typeCombo.getValue());
                if (t != null) {
                    firstHourField.setText(String.valueOf((int) t.getFirstHourCost()));
                    additionalHourField.setText(String.valueOf((int) t.getAdditionalHourCost()));
                    dailyMaxField.setText(String.valueOf((int) t.getDailyMaxCost()));
                }
            } catch (SQLException ex) {
                showAlert("خطا: " + ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("نوع وسیله:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("هزینه ساعت اول:"), 0, 1);
        grid.add(firstHourField, 1, 1);
        grid.add(new Label("هزینه ساعت دوم به بعد:"), 0, 2);
        grid.add(additionalHourField, 1, 2);
        grid.add(new Label("حداکثر هزینه شبانه‌روزی:"), 0, 3);
        grid.add(dailyMaxField, 1, 3);


        dialog.getDialogPane().setContent(grid);
        ButtonType saveBtn = new ButtonType("ذخیره", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("لغو", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(cancelBtn, saveBtn);
        dialog.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // کلید های کنترلی و میانبرها برای منوی تعرفه ها

        // for vehicle type
        typeCombo.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER){
                firstHourField.requestFocus();
                firstHourField.selectAll();
                e.consume();
            }
        });
        // for first hour text field
        firstHourField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.DOWN){
                additionalHourField.requestFocus();
                additionalHourField.selectAll();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                typeCombo.requestFocus();
                e.consume();
            }
        });
        // for an hour after first hour text field
        additionalHourField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.DOWN){
                dailyMaxField.requestFocus();
                dailyMaxField.selectAll();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                firstHourField.requestFocus();
                firstHourField.selectAll();
                e.consume();
            }
        });
        // for daily tariff text field
        dailyMaxField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER ){
                Button btn = (Button) dialog.getDialogPane().lookupButton(saveBtn);
                btn.fire();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                additionalHourField.requestFocus();
                additionalHourField.selectAll();
                e.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    double first = Double.parseDouble(firstHourField.getText());
                    double add = Double.parseDouble(additionalHourField.getText());
                    double daily = Double.parseDouble(dailyMaxField.getText());
                    Tariff newTariff = new Tariff(typeCombo.getValue(), first, add, daily);
                    parkingService.updateTariff(typeCombo.getValue(), newTariff, currentUser.getRole());
                    showAlert("تعرفه با موفقیت به‌روز شد.");
                } catch (Exception ex) {
                    showAlert("مقادیر نامعتبر یا خطا: " + ex.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void updateCapacityLabel() {
        try {
            int current = parkingService.getCurrentOccupancy();
            int max = parkingService.getMaxCapacity();
            capacityLabel.setText(String.format("ظرفیت: %d / %d ( %d جای خالی )", current, max, (max - current)));
        } catch (SQLException e) {
            capacityLabel.setText("خطا در دریافت ظرفیت");
        }
    }

    private void refreshActiveList() {
        try {
            List<ParkingSession> sessions = parkingService.getActiveSessions();
            activeListView.getItems().clear();
            for (ParkingSession s : sessions) {
                String plate = s.getVehicle().getPlateNumber();
                String type = (s.getVehicle().getType() == VehicleType.CAR) ? "خودرو" : "موتور";
                String entryTime = PersianDateUtil.toPersianDateTime(s.getEntryTime());
                activeListView.getItems().add( "پلاک: " + plate + " (" + type + ") - ورود: " + entryTime);
            }
            if (sessions.isEmpty()) {
                activeListView.getItems().add("هیچ وسیله‌ای در پارکینگ نیست.");
            }
            updateCapacityLabel();
        } catch (SQLException e) {
            showAlert("خطا در بارگذاری لیست: " + e.getMessage());
        }
    }

    private void showEntryDialog() {
        // ابتدا نوع وسیله را می‌پرسیم
        ChoiceDialog<VehicleType> typeDialog = new ChoiceDialog<>(VehicleType.CAR, VehicleType.values());
        typeDialog.setTitle("ثبت ورود");
        typeDialog.setHeaderText("نوع وسیله را انتخاب کنید");
        ButtonType chosenBtn = new ButtonType("انتخاب", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("لغو", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType entryBtn = new ButtonType("ثبت", ButtonBar.ButtonData.OK_DONE);
        typeDialog.getDialogPane().getButtonTypes().setAll(chosenBtn, cancelBtn);

        Optional<VehicleType> typeResult = typeDialog.showAndWait();
        if (!typeResult.isPresent()) return;
        VehicleType type = typeResult.get();

        String fullPlate;
        if (type == VehicleType.CAR) {
            // پلاک خودرو: 4 بخش
            TextField part1 = new TextField();
            part1.setPromptText("دو رقم");
            part1.setPrefWidth(50);
            TextField part2 = new TextField();
            part2.setPromptText("حرف");
            part2.setPrefWidth(35);
            TextField part3 = new TextField();
            part3.setPromptText("سه رقم");
            part3.setPrefWidth(80);
            Label iranCode = new Label("ایران");
            TextField part4 = new TextField();
            part4.setPromptText("ایران");
            part4.setPrefWidth(50);

            limitInputToNumbers(part1, 2,true);
            limitInputToNumbers(part3, 3,true);
            limitInputToNumbers(part4, 2,true);
            limitInputToPersianLetters(part2);

            HBox plateBox = new HBox(10, part4,iranCode, part3, part2, part1);
            plateBox.setAlignment(Pos.CENTER);
            plateBox.setPadding(new Insets(10));

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("ورود خودرو");
            dialog.setHeaderText("شماره پلاک را وارد کنید");
            dialog.getDialogPane().setContent(plateBox);
            dialog.getDialogPane().getButtonTypes().addAll(cancelBtn, entryBtn);
            dialog.getDialogPane().setPrefWidth(400);
            dialog.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            dialog.setOnShown(e -> {
                Platform.runLater(() -> {
                    part1.requestFocus();
                    part1.selectAll();
                });
            });

            // جابجایی با Enter
            part1.setOnKeyPressed(ev -> moveToNext(ev, part2));
            part2.setOnKeyPressed(ev -> moveToNext(ev, part3));
            part3.setOnKeyPressed(ev -> moveToNext(ev, part4));
            part4.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    Button btn = (Button) dialog.getDialogPane().lookupButton(entryBtn);
                    btn.fire();
                    ev.consume();
                }
            });

            Optional<ButtonType> res = dialog.showAndWait();
            if (!res.isPresent() || res.get() != entryBtn) return;

            String p1 = part1.getText().trim();
            String p2 = part2.getText().trim();
            String p3 = part3.getText().trim();
            String p4 = part4.getText().trim();

            if (p1.isEmpty() || p2.isEmpty() || p3.isEmpty() || p4.isEmpty()) {
                showAlert("لطفاً همه اجزای پلاک را کامل وارد کنید.");
                return;
            }
            if (!isValidCarPlate(p1, p2, p3, p4)) {
                showAlert("پلاک خودرو نامعتبر است. فرمت صحیح: دو رقم (بدون صفر ) - یک حرف فارسی - سه رقم (بدون صفر ) - دو رقم (رقم اول صفر نباشد)");
                return;
            }
            fullPlate = p4 + "-" + p3 + "-" + p2 + "-" + p1;
            // در غیر این صورت پلاک معتبر است
        } else { // MOTORCYCLE
            // پلاک موتور: دو بخش (سه رقم و پنج رقم)
            TextField part1 = new TextField();
            part1.setPromptText("سه رقم اول");
            part1.setPrefWidth(100);
            TextField part2 = new TextField();
            part2.setPromptText("پنج رقم دوم");
            part2.setPrefWidth(120);

            limitInputToNumbers(part1, 3,true);
            limitInputToNumbers(part2, 5,true);

            VBox plateBox = new VBox(10, part1, part2);
            plateBox.setAlignment(Pos.CENTER);
            plateBox.setPadding(new Insets(10));

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("ورود موتور");
            dialog.setHeaderText("پلاک موتور را وارد کنید");
            dialog.getDialogPane().setContent(plateBox);
            dialog.getDialogPane().getButtonTypes().addAll(entryBtn, cancelBtn);
            dialog.getDialogPane().setPrefWidth(100);
            dialog.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            dialog.setOnShown(e -> {
                Platform.runLater(() -> {
                    part1.requestFocus();
                    part1.selectAll();
                });
            });

            part1.setOnKeyPressed(ev -> moveToNext(ev, part2));
            part2.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    Button btn = (Button) dialog.getDialogPane().lookupButton(entryBtn);
                    btn.fire();
                    ev.consume();
                }
            });

            Optional<ButtonType> res = dialog.showAndWait();
            if (!res.isPresent() || res.get() != entryBtn) return;
            String p1 = part1.getText().trim();
            String p2 = part2.getText().trim();
            fullPlate = p2 + "-" + p1; // فرمت 12345-456
            String motorPlateRegex = "^[1-9][1-9]{4}-[1-9][1-9]{2}$";
            if (!fullPlate.matches(motorPlateRegex)) {
                showAlert("پلاک موتور نامعتبر است.\nفرمت صحیح: سه رقم (بدون صفر) - پنج رقم (بدون صفر)\nمثال: 123-45678");
                return;
            }
            if (p1.isEmpty() || p2.isEmpty()) {
                showAlert("هر دو قسمت پلاک را کامل وارد کنید.");
                return;
            }
        }

        // ثبت ورود با پلاک ساخته شده
        try {
            ParkingSpot assignedSpot = parkingService.registerEntry(fullPlate, type);
            refreshActiveList();
            receiptArea.setText(String.format("✅ ورود ثبت شد.\nشماره پلاک:  %s (%s)\nجای پارک تخصیصی: %s",
                    fullPlate, (type == VehicleType.CAR ? "خودرو" : "موتور"), assignedSpot.getSpotNumber()));
        } catch (Exception ex) {
            showAlert("خطا در ثبت ورود: " + ex.getMessage());
        }
    }

    private void showExitDialog() {
        // ابتدا نوع وسیله را می‌پرسیم (برای خروج نیز نیاز است تا فرمت پلاک مشخص شود)
        ChoiceDialog<VehicleType> typeDialog = new ChoiceDialog<>(VehicleType.CAR, VehicleType.values());
        typeDialog.setTitle("ثبت خروج");
        typeDialog.setHeaderText("نوع وسیله را انتخاب کنید");
        ButtonType chosenBtn = new ButtonType("انتخاب", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("لغو", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType exitBtn = new ButtonType("خروج", ButtonBar.ButtonData.OK_DONE);
        typeDialog.getDialogPane().getButtonTypes().setAll(chosenBtn, cancelBtn);
        Optional<VehicleType> typeResult = typeDialog.showAndWait();
        if (!typeResult.isPresent()) return;
        VehicleType type = typeResult.get();

        String fullPlate;
        if (type == VehicleType.CAR) {
            // پلاک خودرو: همان چهار بخش
            TextField part1 = new TextField();
            part1.setPromptText("دو رقم");
            part1.setPrefWidth(50);
            TextField part2 = new TextField();
            part2.setPromptText("حرف");
            part2.setPrefWidth(35);
            TextField part3 = new TextField();
            part3.setPromptText("سه رقم");
            part3.setPrefWidth(80);
            Label iranCode = new Label("ایران");
            TextField part4 = new TextField();
            part4.setPromptText("دو رقم");
            part4.setPrefWidth(50);

            limitInputToNumbers(part1, 2,true);
            limitInputToNumbers(part3, 3,true);
            limitInputToNumbers(part4, 2,true);
            limitInputToPersianLetters(part2);

            HBox plateBox = new HBox(10, part4, iranCode, part3, part2, part1);
            plateBox.setAlignment(Pos.CENTER);
            plateBox.setPadding(new Insets(10));

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("خروج خودرو");
            dialog.setHeaderText("پلاک خودرو را وارد کنید");
            dialog.getDialogPane().setContent(plateBox);
            dialog.getDialogPane().getButtonTypes().addAll(cancelBtn, exitBtn);
            dialog.getDialogPane().setPrefWidth(400);
            dialog.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);


            dialog.setOnShown(e -> {
                Platform.runLater(() -> {
                    part1.requestFocus();
                    part1.selectAll();
                });
            });

            part1.setOnKeyPressed(ev -> moveToNext(ev, part2));
            part2.setOnKeyPressed(ev -> moveToNext(ev, part3));
            part3.setOnKeyPressed(ev -> moveToNext(ev, part4));
            part4.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    Button btn = (Button) dialog.getDialogPane().lookupButton(exitBtn);
                    btn.fire();
                    ev.consume();
                }
            });

            Optional<ButtonType> res = dialog.showAndWait();
            if (!res.isPresent() || res.get() != exitBtn) return;
            String p1 = part1.getText().trim();
            String p2 = part2.getText().trim();
            String p3 = part3.getText().trim();
            String p4 = part4.getText().trim();

            if (p1.isEmpty() || p2.isEmpty() || p3.isEmpty() || p4.isEmpty()) {
                showAlert("همه اجزای پلاک را کامل وارد کنید.");
                return;
            }
            if (!isValidCarPlate(p1, p2, p3, p4)) {
                showAlert("پلاک خودرو نامعتبر است. فرمت صحیح: دو رقم (بدون صفر ابتدا) - یک حرف فارسی - سه رقم (بدون صفر ابتدا) - دو رقم (رقم اول بدون صفر)");
                return;
            }
            fullPlate = p4 + "-" + p3 + "-" + p2 + "-" + p1;
        } else { // MOTORCYCLE
            TextField part1 = new TextField();
            part1.setPromptText("سه رقم اول");
            part1.setPrefWidth(100);
            TextField part2 = new TextField();
            part2.setPromptText("پنج رقم دوم");
            part2.setPrefWidth(120);

            limitInputToNumbers(part1, 3,true);
            limitInputToNumbers(part2, 5,true);

            VBox plateBox = new VBox(10, part1, part2);
            plateBox.setAlignment(Pos.CENTER);
            plateBox.setPadding(new Insets(10));

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("خروج موتور");
            dialog.setHeaderText("لطفاً پلاک موتور را وارد کنید");
            dialog.getDialogPane().setContent(plateBox);
            dialog.getDialogPane().getButtonTypes().addAll(exitBtn, cancelBtn);
            dialog.getDialogPane().setPrefWidth(100);
            dialog.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            dialog.setOnShown(e -> {
                Platform.runLater(() -> {
                    part1.requestFocus();
                    part1.selectAll();
                });
            });

            part1.setOnKeyPressed(ev -> moveToNext(ev, part2));
            part2.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    Button btn = (Button) dialog.getDialogPane().lookupButton(exitBtn);
                    btn.fire();
                    ev.consume();
                }
            });

            Optional<ButtonType> res = dialog.showAndWait();
            if (!res.isPresent() || res.get() != exitBtn) return;
            String p1 = part1.getText().trim();
            String p2 = part2.getText().trim();
            fullPlate = p2 + "-" + p1; // فرمت 12345-456
            String motorPlateRegex = "^[1-9][0-9]{4}-[1-9][0-9]{2}$";
            if (!fullPlate.matches(motorPlateRegex)) {
                showAlert("پلاک موتور نامعتبر است.\nفرمت صحیح: سه رقم (بدون صفر) - پنج رقم (بدون صفر)\nمثال: 123-45678");
                return;
            }
            if (p1.isEmpty() || p2.isEmpty()) {
                showAlert("هر دو قسمت پلاک را کامل وارد کنید.");
                return;
            }
        }

        // ثبت خروج
        try {
            ParkingTicket ticket = parkingService.registerExit(fullPlate,currentUser.getUsername());
            String entryTime = PersianDateUtil.toPersianDateTime(ticket.getVehicle().getEntryTime());
            String exitTime = PersianDateUtil.toPersianDateTime(ticket.getExitTime());

            String receiptText = String.format(
                    "════════════════════ رسید پارکینگ ════════════════════\n" +
                            "رسید شماره: %s\nپلاک: %s\nنوع: %s\nزمان ورود: %s\nزمان خروج: %s\n" +
                            "مدت توقف: %d دقیقه (%.1f ساعت)\nهزینه کل: %,.0f ریال\n══════════════════════════════════════════════════════",
                    ticket.getReceiptId(), ticket.getVehicle().getPlateNumber(),
                    (ticket.getVehicle().getType() == VehicleType.CAR ? "خودرو" : "موتور"),
                    entryTime, exitTime,
                    ticket.getDurationMinutes(), ticket.getDurationMinutes() / 60.0,
                    ticket.getTotalCost()
            );
            receiptArea.setText(receiptText);
            refreshActiveList();
        } catch (Exception ex) {
            showAlert("خطا در ثبت خروج: " + ex.getMessage());
        }
    }

    // محدودیت ورودی اعداد (حداکثر طول و جلوگیری از صفر ابتدایی)
    private void limitInputToNumbers(TextField field, int maxLength, boolean noLeadingZero) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }
            if (newVal.length() > maxLength) {
                field.setText(newVal.substring(0, maxLength));
                return;
            }
            if (noLeadingZero && newVal.length() > 1 && newVal.startsWith("0")) {
                field.setText(newVal.substring(1));
            }
        });
    }

    // محدودیت ورودی حروف فارسی (فقط یک حرف)
    private void limitInputToPersianLetters(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String persianRegex = "[ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی]*";
            if (!newVal.matches(persianRegex)) {
                field.setText(newVal.replaceAll("[^ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی]", ""));
            }
            if (newVal.length() > 1) {
                field.setText(newVal.substring(0, 1));
            }
        });
    }
    private void moveToNext(KeyEvent event, TextField nextField) {
        if (event.getCode() == KeyCode.ENTER) {
            nextField.requestFocus();
            event.consume();
        }
    }

    private boolean isValidCarPlate(String part1, String part2, String part3, String part4) {
        // بررسی طول
        if (part1.length() != 2 || part3.length() != 3 || part4.length() != 2) return false;
        // بررسی عدم شروع با صفر
        if (part1.startsWith("0") || part3.startsWith("0") || part4.startsWith("0")) return false;
        // اعداد دو رقمی و سه رقمی نیز نباید صفر داشته باشند
        if (part3.contains("0") || part1.contains("0")) return false;
        // بررسی اینکه همه کاراکترهای عددی هستند (قبلاً توسط limitInputToNumbers گرفته شده ولی بازهم چک کنیم)
        if (!part1.matches("\\d{2}") || !part3.matches("\\d{3}") || !part4.matches("\\d{2}")) return false;
        // بررسی حرف فارسی (تنها یک حرف)
        if (!part2.matches("[ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی]")) return false;
        return true;
    }

    private void showParkingSpotsDialog() {
        try {
            refreshSpotsDialog();
        } catch (SQLException e) {
            showAlert("خطا در دریافت جایگاه‌ها: " + e.getMessage());
        }
    }

    private void showParkingSettingsDialog() {
        if (currentUser.getRole() != Role.OWNER && currentUser.getRole() != Role.ADMIN) {
            showAlert("فقط مالک یا ادمین می‌توانند تنظیمات جایگاه را تغییر دهند.");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("تنظیمات جایگاه‌های پارکینگ");
        stage.initModality(Modality.APPLICATION_MODAL); // جلوگیری از تعامل با پنجره اصلی تا بسته شدن این پنجره
        stage.setMinWidth(500);
        stage.setMinHeight(450);

        // لیست بلوک‌ها
        ListView<String> blocksListView = new ListView<>();
        blocksListView.setPrefHeight(250);
        blocksListView.setPrefWidth(400);

        // بارگذاری داده‌ها
        refreshBlockList(blocksListView);  // استفاده از متد کمکی

        // دکمه‌ها
        Button addBtn = new Button("➕ افزودن بلوک");
        Button resizeBtn = new Button("📏 تغییر تعداد جایگاه");
        Button removeBtn = new Button("❌ حذف بلوک");
        Button closeBtn = new Button("بستن");


        //اجرای دکمه ها
        addBtn.setOnAction(e -> {
            showAddBlockDialog(stage, blocksListView);
            refreshBlockList(blocksListView);
        });
        resizeBtn.setOnAction(e -> {
            showResizeBlockDialog(stage, blocksListView);
            refreshBlockList(blocksListView);
        });
        removeBtn.setOnAction(e -> {
            showRemoveBlockDialog(stage, blocksListView);
            refreshBlockList(blocksListView);
        });

        HBox btnRow = new HBox(15, removeBtn, resizeBtn, addBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(10));
        HBox closeRow = new HBox(closeBtn);
        closeRow.setAlignment(Pos.CENTER);

        VBox content = new VBox(15, blocksListView, btnRow, closeRow);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ccc;");

        //کلیدهای میانبر منو تنظیمات جایگاه
        content.setOnKeyPressed(e -> {
            switch (e.getCode()){
                case F1:
                    removeBtn.fire();
                    break;
                case F2:
                    resizeBtn.fire();
                    break;
                case F3:
                    addBtn.fire();
                    break;
                case ESCAPE:
                    closeBtn.fire();
                    break;
                default:
            }
            e.consume();
        });

        Scene scene = new Scene(content);
        stage.setScene(scene);
        stage.show();

        // رویدادها
        closeBtn.setOnAction(e -> stage.close());

        addBtn.setOnAction(e -> {
            showAddBlockDialog(null, blocksListView);
            refreshBlockList(blocksListView);
        });
        resizeBtn.setOnAction(e -> {
            showResizeBlockDialog(null, blocksListView);
            refreshBlockList(blocksListView);
        });
        removeBtn.setOnAction(e -> {
            showRemoveBlockDialog(null, blocksListView);
            refreshBlockList(blocksListView);
        });

        stage.setOnHidden(event -> {
            try {
                refreshSpotsDialog();  // به‌روزرسانی پنجره مشاهده جایگاه‌ها
                updateCapacityLabel();
            } catch (SQLException ex) {
                showAlert("خطا در بازخوانی: " + ex.getMessage());
            }
        });
    }

    private void refreshBlockList(ListView<String> listView) {
        try {
            List<String> blocks = DatabaseManager.getDistinctBlocks();
            List<String> display = new ArrayList<>();
            for (String block : blocks) {
                int count = DatabaseManager.getSpotCountForBlock(block);
                display.add(block + " - تعداد جایگاه: " + count);
            }
            if (display.isEmpty()) {
                display.add("(هیچ بلوکی یافت نشد)");
            }
            listView.getItems().setAll(display);
        } catch (SQLException e) {
            e.printStackTrace();
            listView.getItems().setAll("خطا در بارگذاری: " + e.getMessage());
        }
    }

    private void showAddBlockDialog(Stage parentStage, ListView<String> listView) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("افزودن بلوک جدید");
        dialog.setHeaderText("مشخصات بلوک را وارد کنید");
        dialog.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        TextField blockField = new TextField();
        blockField.setPromptText("حرف بلوک (مثلاً C)");
        TextField countField = new TextField();
        countField.setPromptText("تعداد جایگاه (مثلاً 20)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("حرف بلوک:"), 0, 0);
        grid.add(blockField, 1, 0);
        grid.add(new Label("تعداد جایگاه:"), 0, 1);
        grid.add(countField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        ButtonType addBtn = new ButtonType("افزودن", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("لغو", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, cancelBtn);

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                try {
                    String block = blockField.getText().trim().toUpperCase();
                    int count = Integer.parseInt(countField.getText().trim());
                    if (block.isEmpty() || count <= 0) throw new IllegalArgumentException();
                    DatabaseManager.addBlock(block, count);
                    // بعد از افزودن، لیست را به‌روز می‌کنیم (در رویداد دکمه در Settings انجام می‌شود)
                } catch (NumberFormatException ex) {
                    showAlert("تعداد جایگاه باید عدد باشد.");
                } catch (SQLException ex) {
                    showAlert("خطا: " + ex.getMessage());
                } catch (Exception ex) {
                    showAlert("ورودی نامعتبر.");
                }
            }
            return null;
        });
        dialog.showAndWait();
    }
    private void showRemoveBlockDialog(Stage parentStage, ListView<String> listView) {
        String selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("لطفاً ابتدا یک بلوک را انتخاب کنید.");
            return;
        }
        String blockLetter = selected.substring(0, 1);
        ButtonType yesBtn = new ButtonType("بله", ButtonBar.ButtonData.YES);
        ButtonType noBtn = new ButtonType("خیر", ButtonBar.ButtonData.NO);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "آیا از حذف بلوک " + blockLetter + " و همه جایگاه‌های آن اطمینان دارید؟\n(فقط در صورت خالی بودن امکان‌پذیر است)",
                yesBtn, noBtn);
        confirm.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        confirm.showAndWait().ifPresent(res -> {
            if (res == yesBtn) {
                try {
                    DatabaseManager.removeBlock(blockLetter);
                    // به‌روزرسانی بعداً انجام می‌شود
                } catch (SQLException ex) {
                    showAlert("خطا: " + ex.getMessage());
                }
            }
        });
    }
    private void showResizeBlockDialog(Stage parentStage, ListView<String> listView) {
        String selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("لطفاً ابتدا یک بلوک را انتخاب کنید.");
            return;
        }
        String blockLetter = selected.substring(0, 1);
        int currentCount;
        try {
            currentCount = DatabaseManager.getSpotCountForBlock(blockLetter);
        } catch (SQLException e) {
            showAlert("خطا: " + e.getMessage());
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("تغییر تعداد جایگاه بلوک " + blockLetter);
        dialog.setHeaderText("تعداد فعلی: " + currentCount);
        dialog.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        TextField newCountField = new TextField();
        newCountField.setPromptText("تعداد جدید");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("تعداد جدید جایگاه‌ها:"), 0, 0);
        grid.add(newCountField, 1, 0);

        dialog.getDialogPane().setContent(grid);
        ButtonType resizeBtn = new ButtonType("اعمال", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("لغو", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(resizeBtn, cancelBtn);

        dialog.setResultConverter(btn -> {
            if (btn == resizeBtn) {
                try {
                    int newCount = Integer.parseInt(newCountField.getText().trim());
                    if (newCount < 1) throw new IllegalArgumentException("تعداد باید حداقل 1 باشد.");
                    DatabaseManager.resizeBlock(blockLetter, newCount);
                    // به‌روزرسانی بعداً انجام می‌شود
                } catch (NumberFormatException ex) {
                    showAlert("تعداد را به درستی وارد کنید.");
                } catch (SQLException ex) {
                    showAlert("خطا: " + ex.getMessage());
                } catch (Exception ex) {
                    showAlert("ورودی نامعتبر.");
                }
            }
            return null;
        });
        dialog.showAndWait();
    }
    private void refreshSpotsDialog() throws SQLException {
        List<ParkingSpot> spots = parkingService.getAllParkingSpots();
        StringBuilder sb = new StringBuilder();
        for (ParkingSpot spot : spots) {
            sb.append(spot.getSpotNumber()).append(": ")
                    .append(spot.isOccupied() ? "اشغال (" + spot.getCurrentPlateNumber() + ")" : "خالی")
                    .append("\n");
        }
        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setPrefWidth(400);
        textArea.setPrefHeight(500);

        Button closeBtn = new Button("بستن");
        Button settingsBtn = new Button("⚙️ تنظیمات جایگاه");

        HBox buttonBar = new HBox(15, settingsBtn, closeBtn);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10));

        VBox vbox = new VBox(10, textArea, buttonBar);
        vbox.setPadding(new Insets(10));

        //کلیدهای میانبر نمایش جایگاه ها
        vbox.setOnKeyPressed(e -> {
            switch (e.getCode()){
                case F1:
                    settingsBtn.fire();
                    break;
                case ESCAPE:
                    closeBtn.fire();
                    break;
                default:
            }
            e.consume();
        });
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("جایگاه‌های پارکینگ");
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);

        closeBtn.setOnAction(e -> dialog.close());
        settingsBtn.setOnAction(e -> {
            dialog.close();
            showParkingSettingsDialog();
        });

        dialog.setOnHidden(event -> updateCapacityLabel());
        dialog.showAndWait();
    }

    private void printReceipt() {
        String receiptText = receiptArea.getText();
        if (receiptText.isEmpty()) return;

        VBox printable = new VBox(5);
        printable.setStyle("-fx-padding: 10; -fx-border-color: black; -fx-font-weight: bold;");
        for (String line : receiptText.split("\n")) {
            Label lbl = new Label(line);
            lbl.setFont(Font.font("Monospaced", 11));
            printable.getChildren().add(lbl);
        }
        printable.setAlignment(Pos.TOP_RIGHT);

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(receiptArea.getScene().getWindow())) {
            boolean success = job.printPage(printable);
            if (success) {
                job.endJob();
                showAlert("رسید با موفقیت چاپ شد.");
            } else {
                showAlert("چاپ ناموفق بود.");
            }
        } else {
            showAlert("چاپ‌گر یافت نشد یا چاپ لغو شد.");
        }
    }
    private void openUserManagement() {
        if (userManagementController == null) {
            userManagementController = new UserManagementController(currentUser);
        }
        root.setCenter(userManagementController.getRoot());
    }

    private void openShiftManagement() throws SQLException {
        if (shiftManagementController == null) {
            shiftManagementController = new ShiftManagementController(currentUser);
        }
        root.setCenter(shiftManagementController.getRoot());
    }

    private void openReports() {
        if (reportController == null) {
            reportController = new ReportController(currentUser);
        }
        root.setCenter(reportController.getRoot());
    }

    private void openProfile() {
        if (profileController == null) {
            profileController = new ProfileController(currentUser);
        }
        root.setCenter(profileController.getRoot());
    }

    private void startShiftIfOperator() {
        if (currentUser.getRole() == Role.OPERATOR) {
            try {
                shiftService.startShift(currentUser.getUsername(), currentUser.getRole());
                receiptArea.setText("شیفت کاری شما آغاز شد.");
            } catch (SQLException e) {
                receiptArea.setText("خطا در شروع شیفت: " + e.getMessage());
            }
        }
    }

    private String translateRole(Role role) {
        switch (role) {
            case OWNER: return "مالک";
            case ADMIN: return "ادمین";
            case OPERATOR: return "اپراتور";
            default: return "نامشخص";
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("اطلاع");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        alert.show();
    }

    public Parent getRoot() { return root; }
}