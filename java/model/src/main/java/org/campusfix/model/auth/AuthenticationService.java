package org.campusfix.model.auth;

import java.sql.SQLException;
import java.util.Optional;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.UserDao;

public final class AuthenticationService {
    private final UserDao users;

    public AuthenticationService(UserDao users) {
        this.users = users;
    }

    public Optional<User> signIn(String username, char[] password) throws SQLException {
        if (username.isBlank() || password.length == 0) {
            return Optional.empty();
        }
        return users.findByUsername(username.strip())
                .filter(account -> PasswordVerifier.matches(password, account.salt(), account.passwordHash()))
                .map(UserDao.Account::user);
    }
}
