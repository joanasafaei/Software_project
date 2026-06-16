package com.parking.manager.util;

public class PasswordValidator {

    public static boolean isValid(String password) {
        if (password == null) return false;
        int len = password.length();
        if (len < 8 || len > 20) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    public static String getErrorMessage() {
        return "رمز عبور باید بین ۸ تا ۲۰ کاراکتر باشد و شامل حداقل یک حرف و یک عدد باشد.";
    }
}