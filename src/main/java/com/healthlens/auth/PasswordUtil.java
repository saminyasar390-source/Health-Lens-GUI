package com.healthlens.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Hashes passwords with SHA-256 before they're ever stored, so a stored
 * account never contains a plain-text password (an improvement over the
 * earlier credentials.txt demo, which did).
 *
 * Honest caveat: plain SHA-256 (with no salt, no iteration count) is better
 * than storing plain text, but it's still not what production systems use —
 * real systems use a slow, salted algorithm like bcrypt or Argon2 to resist
 * brute-force guessing. That's a reasonable "future work" note if asked.
 */
public final class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not hash password", e);
        }
    }
}
