package org.campusfix.model.persistence;

import java.sql.SQLException;
import java.util.UUID;
import org.campusfix.model.TestDatabase;
import org.campusfix.model.domain.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class UserDaoIntegrationTest {
    @Test
    void createsReadsUpdatesAndDeletesAnAccount() throws SQLException {
        try (var database = TestDatabase.open()) {
            assertTrue(database.getConnection().getCatalog().endsWith("_test"),
                    "Account writes require a dedicated test database.");
            CrudDao<UserDao.Account> accounts = new UserDao(database);
            String username = String.format("crud-%s", UUID.randomUUID());
            byte[] salt = new byte[16];
            byte[] passwordHash = new byte[32];
            var regular = new User(0, username, User.Role.REGULAR);
            var created = accounts.create(new UserDao.Account(regular, salt, passwordHash));
            long id = created.user().id();

            try {
                assertTrue(id > 0);
                var saved = accounts.read(id).orElseThrow();
                assertEquals(new User(id, username, User.Role.REGULAR), saved.user());

                passwordHash[0] = 1;
                var admin = new User(id, String.format("%s-admin", username), User.Role.ADMIN);
                var updated = new UserDao.Account(admin, salt, passwordHash);
                assertTrue(accounts.update(updated));
                var changed = accounts.read(id).orElseThrow();
                assertEquals(admin, changed.user());
                assertArrayEquals(passwordHash, changed.passwordHash());

                assertTrue(accounts.delete(id));
                assertTrue(accounts.read(id).isEmpty());
                assertFalse(accounts.update(updated));
                assertFalse(accounts.delete(id));
            } finally {
                accounts.delete(id);
            }
        }
    }
}
