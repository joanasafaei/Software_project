package com.parking.manager.controller;

import com.parking.manager.model.*;
import com.parking.manager.service.AuthService;
import com.parking.manager.util.PasswordHasher;
import com.parking.manager.util.PasswordValidator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/**
 * کنترلر مدیریت کاربران (ایجاد، ویرایش، بایگانی)
 * فقط مالک و ادمین دسترسی دارند – محدودیت در سطح متدها اعمال می‌شود
 */
public class UserManagementController {
    private User currentUser;
    private AuthService authService = new AuthService();
    private BorderPane root;
    private TableView<User> userTable;
    private List<User> userList;

    public UserManagementController(User currentUser) {
        this.currentUser = currentUser;
        buildUI();
        loadUsers();
    }

    private void buildUI() {
        root = new BorderPane();
        root.setPadding(new Insets(10));

        Label title = new Label("مدیریت کاربران سیستم");
        title.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;");

        String colUserStr = "نام کاربری";
        String colFullNameStr = "نام کامل";
        String colRoleStr = "نقش";


        userTable = new TableView<>();
        TableColumn<User, String> colUser = new TableColumn<>(colUserStr);
        colUser.setMinWidth(170);
        colUser.setStyle("-fx-alignment: center");
        colUser.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUsername()));
        TableColumn<User, String> colFullName = new TableColumn<>(colFullNameStr);
        colFullName.setMinWidth(170);
        colFullName.setStyle("-fx-alignment: center");
        colFullName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName()));
        TableColumn<User, String> colRole = new TableColumn<>(colRoleStr);
        colRole.setMinWidth(150);
        colRole.setStyle("-fx-alignment: center");
        colRole.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRole().name()));
        userTable.getColumns().addAll(colUser, colFullName, colRole);
        userTable.setPrefHeight(380);
        userTable.setStyle("-fx-font-weight: bold; -fx-text-fill: Black;");
        userTable.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        Button btnAdd = new Button("افزودن کاربر جدید");
        Button btnEdit = new Button("ویرایش کاربر");
        Button btnArchive = new Button("بایگانی کاربر");
        Button btnRefresh = new Button("بروزرسانی");

        HBox hBox = new HBox(15, btnRefresh, btnArchive, btnEdit, btnAdd);
        hBox.setAlignment(Pos.CENTER);
        hBox.setPadding(new Insets(15));

        btnAdd.setOnAction(e -> showAddEditDialog(null));
        btnEdit.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) showAddEditDialog(selected);
            else showAlert("لطفاً یک کاربر را انتخاب کنید.");
        });
        btnArchive.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) archiveUser(selected);
            else showAlert("لطفاً یک کاربر را انتخاب کنید.");
        });
        btnRefresh.setOnAction(e -> loadUsers());

        VBox vbox = new VBox(10, title, userTable, hBox);
        vbox.setPadding(new Insets(10));
        vbox.setAlignment(Pos.TOP_RIGHT);
        root.setCenter(vbox);

        //کلیدهای میانبر منو شیفت
        root.setOnKeyPressed(e -> {
            switch (e.getCode()){
                case F1:
                    btnRefresh.fire();
                    break;
                case F2:
                    btnArchive.fire();
                    break;
                case F3:
                    btnEdit.fire();
                    break;
                case F4:
                    btnAdd.fire();
                    break;
                default:
            }
            e.consume();
        });
    }

    private void loadUsers() {
        try {
            userList = authService.listActiveUsers(currentUser.getRole());
            userTable.setItems(FXCollections.observableArrayList(userList));
        } catch (SQLException | SecurityException e) {
            showAlert("خطا در بارگذاری کاربران: " + e.getMessage());
        }
    }

    private void showAddEditDialog(User existingUser) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existingUser == null ? "افزودن کاربر جدید" : "ویرایش کاربر");
        dialog.setHeaderText(null);

        TextField usernameField = new TextField();
        TextField fullNameField = new TextField();
        PasswordField passwordField = new PasswordField();
        ComboBox<Role> roleCombo = new ComboBox<>(FXCollections.observableArrayList(Role.values()));

        if (existingUser != null) {
            usernameField.setText(existingUser.getUsername());
            usernameField.setDisable(true);
            fullNameField.setText(existingUser.getFullName());
            roleCombo.setValue(existingUser.getRole());
            passwordField.setPromptText("در صورت تمایل رمز جدید وارد کنید");
        } else {passwordField.setPromptText("رمز عبور (الزامی)");
        }

        GridPane grid = new GridPane();
        grid.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("نام کاربری:"), usernameField);
        grid.addRow(1, new Label("نام کامل:"), fullNameField);
        grid.addRow(2, new Label("رمز عبور:"), passwordField);
        grid.addRow(3, new Label("نقش:"), roleCombo);

        // کلیدهای میانبر برای dialog
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.DOWN){
                fullNameField.requestFocus();
                fullNameField.selectAll();
                e.consume();
            }
        });
        fullNameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.DOWN){
                passwordField.requestFocus();
                passwordField.selectAll();
                e.consume();
            }
            if (e.getCode() == KeyCode.UP){
                usernameField.requestFocus();
                usernameField.selectAll();
                e.consume();
            }
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.DOWN){
                roleCombo.requestFocus();
                e.consume();
            }
            if (e.getCode() == KeyCode.UP){
                fullNameField.requestFocus();
                fullNameField.selectAll();
                e.consume();
            }
        });
        roleCombo.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT){
                passwordField.requestFocus();
                passwordField.selectAll();
                e.consume();
            }
        });

        dialog.getDialogPane().setContent(grid);
        ButtonType saveBtn = new ButtonType("ذخیره", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("لغو", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, cancelBtn);
        dialog.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        if (existingUser == null){
            dialog.setOnShown(e -> {
                Platform.runLater(() -> {
                    usernameField.requestFocus();
                    usernameField.selectAll();
                });
            });
        }else {
            dialog.setOnShown(e -> {
                Platform.runLater(() -> {
                    fullNameField.requestFocus();
                    fullNameField.selectAll();
                });
            });
        }


        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    String username = usernameField.getText().trim();
                    String fullName = fullNameField.getText().trim();
                    Role role = roleCombo.getValue();
                    if (existingUser == null) {
                        String password = passwordField.getText();
                        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty() || role == null) {
                            throw new IllegalArgumentException("همه فیلدها الزامی است.");
                        }if (!PasswordValidator.isValid(password)) {
                            showAlert(PasswordValidator.getErrorMessage());
                        }else {
                            String hash = PasswordHasher.hash(password);
                            User newUser = new User(username, hash, role, fullName, false);
                            authService.createUser(newUser, currentUser.getRole());
                        }} else {
                        // ویرایش: فقط نام کامل و نقش قابل تغییر است (رمز به صورت جداگانه در پروفایل)
                        authService.updateUser(existingUser.getUsername(), fullName, role, false, currentUser.getRole());
                        // اگر رمز جدید وارد شده باشد، تغییر بده
                        String newPassword = passwordField.getText();
                        if (PasswordValidator.isValid(newPassword)) {
                            authService.changePassword(existingUser.getUsername(), "", newPassword); // نیاز به رمز فعلی ندارد – مشکل امنیتی!
                        }else {
                            showAlert(PasswordValidator.getErrorMessage());
                        }
                    }
                    loadUsers();
                } catch (Exception ex) {
                    showAlert("خطا: " + ex.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void archiveUser(User user) {
        ButtonType yesBtn = new ButtonType("بله", ButtonBar.ButtonData.OK_DONE);
        ButtonType noBtn = new ButtonType("خیر", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("بایگانی کاربر");
        confirm.setHeaderText("آیا کاربر " + user.getUsername() + " بایگانی شود؟");
        confirm.setContentText("کاربر بایگانی شده قادر به ورود نخواهد بود.");
        confirm.getButtonTypes().setAll(yesBtn, noBtn);
        confirm.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    authService.updateUser(user.getUsername(), user.getFullName(), user.getRole(), true, currentUser.getRole());
                    loadUsers();
                } catch (Exception e) {
                    showAlert("خطا در بایگانی: " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public Parent getRoot() { return root; }
}
