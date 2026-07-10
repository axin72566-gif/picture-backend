package com.example.picturebackend.utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtils {

    private static final int COST = 12;

    private PasswordUtils() {
    }

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(COST));
    }

    public static boolean check(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}