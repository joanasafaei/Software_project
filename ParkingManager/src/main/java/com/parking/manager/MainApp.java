package com.parking.manager;

import com.parking.manager.controller.LoginController;
import com.parking.manager.database.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * کلاس اصلی برنامه سامانه مدیریت پارکینگ
 * با امکانات جانبی: ذخیره تنظیمات پنجره، نمایش اسپلش، مدیریت خروج
 */
public class MainApp extends Application {
    public static void main(String[] args) {
        System.setProperty("prism.order", "sw");
        launch(args);
    }

    private static final String PREFS_NODE = "com.parking.manager";
    private static final String KEY_WINDOW_X = "windowX";
    private static final String KEY_WINDOW_Y = "windowY";
    private static final String KEY_WINDOW_WIDTH = "windowWidth";
    private static final String KEY_WINDOW_HEIGHT = "windowHeight";


    private Stage primaryStage;
    private Preferences prefs;

    @Override
    public void init() throws Exception {
        // قبل از start: می‌توان اسپلش اسکرین یا بررسی دیتابیس را انجام داد
        System.out.println("در حال راه‌اندازی سامانه مدیریت پارکینگ...");
        // بررسی اولیه دیتابیس (در صورت خطای بحرانی، برنامه خاتمه یابد)
        try {
            DatabaseManager.initializeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "خطا در اتصال به دیتابیس! برنامه بسته می‌شود.\n" + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
                Platform.exit();
            });
            throw e;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        prefs = Preferences.userRoot().node(PREFS_NODE);

        // تنظیم آیکون برنامه (در صورت وجود فایل)
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/css/Images/icon.jpg")));
        } catch (Exception e) {
            // نادیده گرفته شود – آیکون پیش‌فرض استفاده می‌شود
        }

        // بارگذاری موقعیت و اندازه ذخیره شده پنجره
        double winX = prefs.getDouble(KEY_WINDOW_X, 200);
        double winY = prefs.getDouble(KEY_WINDOW_Y, 100);
        double winWidth = prefs.getDouble(KEY_WINDOW_WIDTH, 1100);
        double winHeight = prefs.getDouble(KEY_WINDOW_HEIGHT, 750);

        primaryStage.setX(winX);
        primaryStage.setY(winY);
        primaryStage.setWidth(winWidth);
        primaryStage.setHeight(winHeight);

        // جلوگیری از کوچک‌تر شدن بیش از حد
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);

        // ذخیره موقعیت هنگام بسته شدن
        primaryStage.setOnCloseRequest(this::handleCloseRequest);

        LoginController loginController = new LoginController();
        loginController.start(primaryStage);
    }

    /**
     * ذخیره موقعیت پنجره قبل از بسته شدن، و خروج ایمن
     */
    private void handleCloseRequest(WindowEvent event) {
        // ذخیره موقعیت و اندازه پنجره
        prefs.putDouble(KEY_WINDOW_X, primaryStage.getX());
        prefs.putDouble(KEY_WINDOW_Y, primaryStage.getY());
        prefs.putDouble(KEY_WINDOW_WIDTH, primaryStage.getWidth());
        prefs.putDouble(KEY_WINDOW_HEIGHT, primaryStage.getHeight());

        // اگر کاربر لاگین کرده و اپراتور است، شیفت او را ببندیم
        // به این منظور می‌توان یک singleton از کاربر جاری نگه داشت.
        // برای سادگی، در اینجا فقط یک تأیید خروج می‌گیریم.

        // تأیید خروج
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "آیا از خروج از برنامه اطمینان دارید؟",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            Platform.exit();
        } else {
            event.consume(); // لغو بسته شدن
        }
    }


    @Override
    public void stop() throws Exception {
        // هنگام خروج کامل برنامه (در صورتی که کاربر درخواست خروج داده باشد)
        System.out.println("برنامه بسته شد.");
        super.stop();
    }
}