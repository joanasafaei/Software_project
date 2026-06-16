package com.parking.manager.controller;

import com.parking.manager.model.User;
import com.parking.manager.service.AuthService;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * کنترلر صفحه ورود
 * پس از احراز هویت موفق، صفحه اصلی متناسب با نقش کاربر را باز می‌کند
 */
public class LoginController {
    private AuthService authService = new AuthService();
    private Stage primaryStage;

    public void start(Stage stage) {
        this.primaryStage = stage;

        // ایجاد عناصر ورودی
        Label titleLabel = new Label("سامانه مدیریت پارکینگ");
        titleLabel.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;");

        Label userLabel = new Label("نام کاربری:");
        TextField usernameField = new TextField();
        usernameField.setMinWidth(450);
        usernameField.setPromptText("نام کاربری");
        usernameField.setAlignment(Pos.CENTER);


        Label passLabel = new Label("رمز عبور:");
        PasswordField passwordField = new PasswordField();
        passwordField.setMinWidth(450);
        passwordField.setPromptText("رمز عبور");
        passwordField.setAlignment(Pos.CENTER);
        // محدودیت طول به ۲۰ کاراکتر
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            if (change.getControlNewText().length() <= 20) {
                return change;
            }
            return null;
        });
        passwordField.setTextFormatter(formatter);

        Button loginBtn = new Button("ورود");
        Button exitBtn = new Button("خروج");
        loginBtn.setMinWidth(150);
        exitBtn.setMinWidth(150);

        // کلیدهای ورود و خروج را کنار هم میزاریم
        HBox btnBox = new HBox(20,  exitBtn, loginBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(10));


        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red; -fx-background-color: rgba(255,255,255,0); -fx-font-size: 14; -fx-alignment: center;");

        // چیدمان با GridPane
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(usernameField, 0, 0);
        grid.add(userLabel, 1, 0);
        grid.add(passwordField, 0, 1);
        grid.add(passLabel, 1, 1);


        // وسط‌چین افقی در سلول
        GridPane.setHalignment(userLabel, HPos.CENTER);
        GridPane.setHalignment(passLabel, HPos.CENTER);

        // وسط‌چین عمودی در سلول
        GridPane.setValignment(userLabel, VPos.CENTER);
        GridPane.setValignment(passLabel, VPos.CENTER);

        VBox vBox = new VBox(10, titleLabel, grid, btnBox, messageLabel);
        vBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.5);");
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(20));

        VBox root = new VBox(0,vBox);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 500, 400);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("ورود به سامانه مدیریت پارکینگ");
        primaryStage.show();

        // رویداد دکمه ورود
        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("لطفاً نام کاربری و رمز عبور را وارد کنید.");
                return;
            }
            try {
                User user = authService.login(username, password);
                if (user != null) {
                    // ورود موفق: بستن صفحه لاگین و باز کردن صفحه اصلی
                    MainController mainController = new MainController(user);
                    Scene mainScene = new Scene(mainController.getRoot(), 1100, 750);
                    mainScene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
                    primaryStage.setScene(mainScene);
                } else {
                    messageLabel.setText("نام کاربری یا رمز عبور اشتباه است.");
                }
            } catch (Exception ex) {
                messageLabel.setText("خطا در ارتباط با دیتابیس: " + ex.getMessage());
            }
        });

        exitBtn.setOnAction(e -> primaryStage.close());

        //کلیدهای میانبر منو لاگین
        root.setOnKeyPressed(e -> {
            switch (e.getCode()){
                case ESCAPE:
                    exitBtn.fire();
                    break;
                case ENTER:
                    loginBtn.fire();
                    break;
                default:
            }
            e.consume();
        });
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.DOWN){
                passwordField.requestFocus();
                passwordField.selectAll();
                e.consume();
            }
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP){
                usernameField.requestFocus();
                usernameField.selectAll();
                e.consume();
            }
        });
    }


}
