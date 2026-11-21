package org.campusfix.model.auth;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordVerifierTest {
    @Test
    void verifiesTheSharedPasswordFormat() {
        var hex = HexFormat.of();
        byte[] salt = hex.parseHex("00112233445566778899aabbccddeeff");
        byte[] passwordHash = hex.parseHex("339add849f6f8177bd35d90372c1d7e143c69a9109f4c3c7964175349f488d53");

        assertTrue(PasswordVerifier.matches("caf\u00e9-demo".toCharArray(), salt, passwordHash));
        assertFalse(PasswordVerifier.matches("wrong-password".toCharArray(), salt, passwordHash));
    }
}
