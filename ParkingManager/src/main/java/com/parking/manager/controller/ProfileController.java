package com.parking.manager.controller;

import com.parking.manager.model.User;
import com.parking.manager.service.AuthService;
import com.parking.manager.util.PasswordValidator;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * کنترلر پروفایل کاربر – تغییر نام کاربری و رمز عبور
 */
public class ProfileController {
    private User currentUser;
    private AuthService authService = new AuthService();
    private BorderPane root;

    public ProfileController(User currentUser) {
        this.currentUser = currentUser;
        buildUI();
    }

    private void buildUI() {
        root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("پروفایل کاربری");
        title.setAlignment(Pos.CENTER);
        title.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;");

        //متن های لازم در تنظیمات
        Label usernameSetLabel = new Label("تغییر نام کاربری:");
        usernameSetLabel.setAlignment(Pos.CENTER_RIGHT);
        Label newUsernameLabel = new Label("نام کاربری جدید:");
        newUsernameLabel.setAlignment(Pos.CENTER_RIGHT);
        Label passForUserLabel = new Label("رمز عبور فعلی:");
        passForUserLabel.setAlignment(Pos.CENTER_RIGHT);
        Label passSetLabel = new Label("تغییر رمز عبور:");
        passSetLabel.setAlignment(Pos.CENTER_RIGHT);
        Label oldPassLabel = new Label("رمز فعلی:");
        oldPassLabel.setAlignment(Pos.CENTER_RIGHT);
        Label newPassLabel = new Label("رمز جدید:");
        newPassLabel.setAlignment(Pos.CENTER_RIGHT);
        Label confirmPassLabel = new Label("تکرار رمز جدید:");
        confirmPassLabel.setAlignment(Pos.CENTER_RIGHT);


        // بخش تغییر نام کاربری
        Label oldUserLabel = new Label("نام کاربری فعلی: \t" + currentUser.getUsername());
        oldUserLabel.setAlignment(Pos.CENTER_RIGHT);

        TextField newUsernameField = new TextField();
        newUsernameField.setAlignment(Pos.CENTER);
        newUsernameField.setPromptText("نام کاربری جدید");

        PasswordField passForUser = new PasswordField();
        passForUser.setAlignment(Pos.CENTER);
        passForUser.setPromptText("رمز عبور فعلی");
        // محدودیت طول رمز به ۲۰ کاراکتر
        TextFormatter<String> userPassFormatter = new TextFormatter<>(change -> {
            if (change.getControlNewText().length() <= 20) {
                return change;
            }
            return null;
        });
        passForUser.setTextFormatter(userPassFormatter);

        Button changeUserBtn = new Button("تغییر نام کاربری");
        changeUserBtn.setAlignment(Pos.CENTER);

        // بخش تغییر رمز عبور
        PasswordField oldPassField = new PasswordField();
        oldPassField.setPromptText("رمز فعلی");
        oldPassField.setAlignment(Pos.CENTER);
        // محدودیت طول رمز به ۲۰ کاراکتر
        TextFormatter<String> passOldFormatter = new TextFormatter<>(change -> {
            if (change.getControlNewText().length() <= 20) {
                return change;
            }
            return null;
        });
        oldPassField.setTextFormatter(passOldFormatter);

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("رمز جدید");
        newPassField.setAlignment(Pos.CENTER);
        // محدودیت طول رمز به ۲۰ کاراکتر
        TextFormatter<String> passNewFormatter = new TextFormatter<>(change -> {
            if (change.getControlNewText().length() <= 20) {
                return change;
            }
            return null;
        });
        newPassField.setTextFormatter(passNewFormatter);

        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("تکرار رمز جدید");
        confirmPassField.setAlignment(Pos.CENTER);
        // محدودیت طول رمز به ۲۰ کاراکتر
        TextFormatter<String> passConfirmFormatter = new TextFormatter<>(change -> {
            if (change.getControlNewText().length() <= 20) {
                return change;
            }
            return null;
        });
        confirmPassField.setTextFormatter(passConfirmFormatter);

        Button changePassBtn = new Button("تغییر رمز عبور");
        changePassBtn.setAlignment(Pos.CENTER);

        Label empty = new Label();
        empty.setStyle("-fx-text-fill: Aqua; -fx-text-origin: black; -fx-background-color: rgba(255,255,255,0); -fx-font-size: 16; -fx-alignment: center;");

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: Aqua; -fx-text-origin: black; -fx-background-color: rgba(255,255,255,0); -fx-font-size: 16; -fx-alignment: center;");
        messageLabel.setAlignment(Pos.CENTER);

        // برای خط بین تنظیمات
        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        separator1.setStyle("-fx-background-color: black; -fx-border-width: 5px");
        separator2.setStyle("-fx-background-color: black; -fx-border-width: 5px");

        GridPane grid = new GridPane();
        grid.setMaxWidth(700);
        grid.setStyle("-fx-border-color: black; -fx-background-color: rgba(255, 255, 255, 0.5); -fx-border-width: 3px");
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(10));
        grid.addRow(0, usernameSetLabel);
        grid.addRow(1, empty, oldUserLabel);
        grid.addRow(2, newUsernameField, newUsernameLabel);
        grid.addRow(3, passForUser, passForUserLabel);
        grid.addRow(4, changeUserBtn);
        grid.addRow(5, separator1, separator2);
        grid.addRow(6, passSetLabel);
        grid.addRow(7, oldPassField, oldPassLabel);
        grid.addRow(8, newPassField, newPassLabel);
        grid.addRow(9, confirmPassField, confirmPassLabel);
        grid.addRow(10, changePassBtn);


        // وسط‌چین افقی در سلول
        GridPane.setHalignment(usernameSetLabel, HPos.RIGHT);
        GridPane.setHalignment(oldUserLabel, HPos.RIGHT);
        GridPane.setHalignment(newUsernameField, HPos.RIGHT);
        GridPane.setHalignment(newUsernameLabel, HPos.RIGHT);
        GridPane.setHalignment(passForUser, HPos.RIGHT);
        GridPane.setHalignment(passForUserLabel, HPos.RIGHT);
        GridPane.setHalignment(changeUserBtn, HPos.RIGHT);
        GridPane.setHalignment(passSetLabel, HPos.RIGHT);
        GridPane.setHalignment(oldPassField, HPos.RIGHT);
        GridPane.setHalignment(oldPassLabel, HPos.RIGHT);
        GridPane.setHalignment(newPassField, HPos.RIGHT);
        GridPane.setHalignment(newPassLabel, HPos.RIGHT);
        GridPane.setHalignment(confirmPassField, HPos.RIGHT);
        GridPane.setHalignment(confirmPassLabel, HPos.RIGHT);
        GridPane.setHalignment(changePassBtn, HPos.RIGHT);

        // وسط‌چین عمودی در سلول
        GridPane.setValignment(usernameSetLabel, VPos.CENTER);
        GridPane.setValignment(oldUserLabel, VPos.CENTER);
        GridPane.setValignment(newUsernameField, VPos.CENTER);
        GridPane.setValignment(newUsernameLabel, VPos.CENTER);
        GridPane.setValignment(passForUser, VPos.CENTER);
        GridPane.setValignment(passForUserLabel, VPos.CENTER);
        GridPane.setValignment(changeUserBtn, VPos.CENTER);
        GridPane.setValignment(passSetLabel, VPos.CENTER);
        GridPane.setValignment(oldPassField, VPos.CENTER);
        GridPane.setValignment(oldPassLabel, VPos.CENTER);
        GridPane.setValignment(newPassField, VPos.CENTER);
        GridPane.setValignment(newPassLabel, VPos.CENTER);
        GridPane.setValignment(confirmPassField, VPos.CENTER);
        GridPane.setValignment(confirmPassLabel, VPos.CENTER);
        GridPane.setValignment(changePassBtn, VPos.CENTER);

        VBox vBox = new VBox(10, title, grid, messageLabel);
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(5));
        root.setCenter(vBox);

        // رویدادها
        changeUserBtn.setOnAction(e -> {
            String newUsername = newUsernameField.getText().trim();
            String password = passForUser.getText();

            if (newUsername.isEmpty() || password.isEmpty()) {
                messageLabel.setText("لطفاً نام کاربری جدید و رمز عبور را وارد کنید.");
                return;
            }
            if (!PasswordValidator.isValid(password)) {
                messageLabel.setText(PasswordValidator.getErrorMessage());
                return;
            }
            try {
                authService.changeUsername(currentUser.getUsername(), newUsername, password);
                messageLabel.setText("نام کاربری با موفقیت تغییر کرد. لطفاً دوباره وارد شوید.");
                // می‌توان بعد از چند ثانیه برنامه را بست یا صفحه لاگین را دوباره نمایش داد
                new Alert(Alert.AlertType.INFORMATION, "نام کاربری تغییر یافت. لطفاً دوباره وارد شوید.").showAndWait();
                System.exit(0); // ساده: خروج از برنامه
            } catch (Exception ex) {
                messageLabel.setText("خطا: " + ex.getMessage());
            }
        });

        changePassBtn.setOnAction(e -> {
            String oldPass = oldPassField.getText();
            String newPass = newPassField.getText();
            String confirm = confirmPassField.getText();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                messageLabel.setText("رمزها نمی توانند خالی باشند.");
                return;
            }
            if (!newPass.equals(confirm)){
                messageLabel.setText("رمز جدید و تکرار آن مطابقت ندارد.");
                return;
            }
            if (!PasswordValidator.isValid(oldPass) || !PasswordValidator.isValid(newPass) || !PasswordValidator.isValid(confirm)) {
                messageLabel.setText(PasswordValidator.getErrorMessage());
                return;
            }
            try {
                authService.changePassword(currentUser.getUsername(), oldPass, newPass);
                messageLabel.setText("رمز عبور با موفقیت تغییر کرد.");
                oldPassField.clear(); newPassField.clear(); confirmPassField.clear();
            } catch (Exception ex) {
                messageLabel.setText("خطا: " + ex.getMessage());
            }
        });

        //کلیدهای میانبر منو پروفایل

        //for change username
        newUsernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER){
                passForUser.requestFocus();
                passForUser.selectAll();
                e.consume();
            }
        });
        passForUser.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER){
                changeUserBtn.fire();
                e.consume();
            }else if (e.getCode() == KeyCode.UP){
                newUsernameField.requestFocus();
                newUsernameField.selectAll();
                e.consume();
            }
        });

        // for change password
        oldPassField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER){
                newPassField.requestFocus();
                newPassField.selectAll();
                e.consume();
            }
        });
        newPassField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER){
                confirmPassField.requestFocus();
                confirmPassField.selectAll();
                e.consume();
            }else if (e.getCode() == KeyCode.UP){
                oldPassField.requestFocus();
                oldPassField.selectAll();
                e.consume();
            }
        });
        confirmPassField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER){
                changePassBtn.fire();
                e.consume();
            }else if(e.getCode() == KeyCode.UP){
                newPassField.requestFocus();
                newPassField.selectAll();
                e.consume();
            }
        });


    }

    public Parent getRoot() { return root; }
}
