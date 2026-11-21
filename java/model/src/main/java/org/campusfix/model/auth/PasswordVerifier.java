package org.campusfix.model.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class PasswordVerifier {
    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH_BITS = 256;

    private PasswordVerifier() {
    }

    static boolean matches(char[] password, byte[] salt, byte[] passwordHash) {
        var spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(passwordHash, derivedKey);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password verification is unavailable.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
