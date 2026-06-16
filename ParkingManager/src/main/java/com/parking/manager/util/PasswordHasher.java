package com.parking.manager.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * کلاس ابزاری برای هش کردن رمز عبور با SHA-256
 * رمزهای ذخیره شده در دیتابیس به صورت هش شده نگهداری می‌شوند
 */
public class PasswordHasher {

    /**
     * دریافت رمز عبور به صورت متن ساده و برگرداندن هش SHA-256 (به صورت هگزادسیمال)
     * @param plainPassword رمز عبور ساده
     * @return رشته هش شده ۶۴ کاراکتری
     */
    public static String hash(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // تبدیل آرایه بایت به رشته هگزادسیمال
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}