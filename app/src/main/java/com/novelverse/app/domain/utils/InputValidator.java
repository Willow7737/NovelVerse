package com.novelverse.app.domain.utils;

public class InputValidator {
    public static String sanitizeEmail(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }
    public static String sanitizeUsername(String raw) {
        return raw == null ? "" : raw.trim();
    }
    public static boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }
    public static boolean isValidUsername(String username) {
        return username != null && username.length() >= 3 && username.length() <= 20;
    }
}
