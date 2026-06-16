package com.parking.manager.service;

import com.parking.manager.database.DatabaseManager;
import com.parking.manager.model.Role;
import com.parking.manager.model.User;
import com.parking.manager.util.PasswordHasher;
import com.parking.manager.util.PasswordValidator;

import java.sql.SQLException;
import java.util.List;

/**
 * سرویس احراز هویت و مدیریت کاربران
 * شامل ورود، ایجاد/ویرایش کاربران، تغییر رمز و نام کاربری
 */
public class AuthService {

    /**
     * ورود به سیستم با نام کاربری و رمز عبور (رمز به صورت هش مقایسه می‌شود)
     * @return User در صورت موفقیت، در غیر این‌صورت null
     */
    public User login(String username, String plainPassword) throws SQLException {
        User user = DatabaseManager.getUserByUsername(username);
        if (user != null) {
            String hashedInput = PasswordHasher.hash(plainPassword);
            if (user.getPasswordHash().equals(hashedInput)) {
                return user;
            }
        }
        return null;
    }

    /**
     * ایجاد کاربر جدید (فقط OWNER یا ADMIN مجاز هستند)
     */

    public void createUser(User newUser, Role requesterRole) throws SQLException {
        if (requesterRole != Role.OWNER && requesterRole != Role.ADMIN) {
            throw new SecurityException("دسترسی غیرمجاز: فقط مالک یا ادمین می‌توانند کاربر ایجاد کنند.");
        }
        // اعتبارسنجی رمز عبور (قبل از هش کردن)
        if (!PasswordValidator.isValid(newUser.getPasswordHash())) {
            throw new IllegalArgumentException(PasswordValidator.getErrorMessage());
        }
        // بررسی عدم تکراری بودن نام کاربری
        if (DatabaseManager.getUserByUsername(newUser.getUsername()) != null) {
            throw new SQLException("نام کاربری تکراری است.");
        }
        DatabaseManager.createUser(newUser);
    }



    /**
     * ویرایش اطلاعات کاربر (فقط OWNER مجاز است)
     */
    public void updateUser(String username, String newFullName, Role newRole, boolean archived, Role requesterRole) throws SQLException {
        if (requesterRole != Role.OWNER) {
            throw new SecurityException("دسترسی غیرمجاز: فقط مالک می‌تواند کاربران را ویرایش کند.");
        }
        DatabaseManager.updateUser(username, newFullName, newRole, archived);
    }

    /**
     * تغییر رمز عبور کاربر جاری (با احراز هویت رمز فعلی)
     */
    public void changePassword(String username, String oldPlainPassword, String newPlainPassword) throws SQLException {
        User user = login(username, oldPlainPassword);
        if (user == null) {
            throw new SecurityException("رمز فعلی اشتباه است.");
        }

        // اعتبارسنجی رمز جدید
        if (!PasswordValidator.isValid(newPlainPassword)) {
            throw new IllegalArgumentException(PasswordValidator.getErrorMessage());
        }

        String newHash = PasswordHasher.hash(newPlainPassword);
        DatabaseManager.changePassword(username, newHash);
    }

    /**
     * تغییر نام کاربری (با احراز هویت رمز عبور)
     */
    public void changeUsername(String oldUsername, String newUsername, String password) throws SQLException {
        // ابتدا اطمینان از صحت رمز
        if (login(oldUsername, password) == null) {
            throw new SecurityException("رمز عبور اشتباه است.");
        }
        // بررسی تکراری نبودن نام جدید
        if (DatabaseManager.getUserByUsername(newUsername) != null) {
            throw new SQLException("نام کاربری جدید تکراری است.");
        }
        DatabaseManager.changeUsername(oldUsername, newUsername);
    }

    /**
     * دریافت لیست کاربران فعال (غیر بایگانی)
     */
    public List<User> listActiveUsers(Role requesterRole) throws SQLException {
        if (requesterRole == Role.OPERATOR) {
            throw new SecurityException("اپراتور مجاز به دیدن لیست کاربران نیست.");
        }
        return DatabaseManager.getAllActiveUsers();
    }
}
