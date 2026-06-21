package org.example.vendly_remastered.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    /**
     * Hashes a plain text password using a secure gensalt work factor.
     * Use this when creating or updating a user account.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Verifies if a plain text password matches a stored BCrypt hash.
     * This is what you will call inside your authenticate query logic!
     */
    public static boolean checkPassword(String plainPassword, String storedHash) {
        if (storedHash == null || !storedHash.startsWith("$2a$")) {
            return false; // Prevent crashes if database hash is empty or invalid
        }
        return BCrypt.checkpw(plainPassword, storedHash);
    }

}
