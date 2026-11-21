package org.campusfix.model.auth;

import java.sql.SQLException;
import org.campusfix.model.TestDatabase;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.UserDao;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class AuthenticationServiceIntegrationTest {
    @Test
    void signsInSeededAccountsAndRejectsInvalidCredentials() throws SQLException {
        try (var database = TestDatabase.open()) {
            var authentication = new AuthenticationService(new UserDao(database));

            var alice = authentication.signIn(" alice ", "alice-demo".toCharArray()).orElseThrow();
            var bob = authentication.signIn("bob", "bob-demo".toCharArray()).orElseThrow();
            var admin = authentication.signIn("admin", "admin-demo".toCharArray()).orElseThrow();

            assertEquals("alice", alice.username());
            assertEquals(User.Role.REGULAR, alice.role());
            assertEquals(User.Role.REGULAR, bob.role());
            assertEquals(User.Role.ADMIN, admin.role());
            assertTrue(authentication.signIn("admin", "wrong-password".toCharArray()).isEmpty());
            assertTrue(authentication.signIn("unknown", "wrong-password".toCharArray()).isEmpty());
        }
    }
}
