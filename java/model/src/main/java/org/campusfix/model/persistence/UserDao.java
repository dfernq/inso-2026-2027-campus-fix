package org.campusfix.model.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.campusfix.model.domain.User;

public final class UserDao implements CrudDao<UserDao.Account> {
    private static final String ACCOUNT_COLUMNS = "id, username, role, password_salt, password_hash";

    private static final RowMapper<Account> ACCOUNT_MAPPER = new RowMapper<>() {
        @Override
        Account mapRow(ResultSet row) throws SQLException {
            var user = new User(row.getLong("id"), row.getString("username"),
                    User.Role.valueOf(row.getString("role")));
            return new Account(user, row.getBytes("password_salt"), row.getBytes("password_hash"));
        }
    };

    public record Account(User user, byte[] salt, byte[] passwordHash) {
    }

    private final DatabaseConnection database;

    public UserDao(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Account create(Account account) throws SQLException {
        String sql = String.format("""
                INSERT INTO users (username, role, password_salt, password_hash)
                VALUES (?, ?, ?, ?)
                RETURNING %s
                """, ACCOUNT_COLUMNS);
        try (var statement = database.getConnection().prepareStatement(sql)) {
            bindAccount(statement, account);
            return ACCOUNT_MAPPER.queryOne(statement)
                    .orElseThrow(() -> new SQLException("Insert returned no account."));
        }
    }

    @Override
    public Optional<Account> read(long id) throws SQLException {
        String sql = String.format("SELECT %s FROM users WHERE id = ?", ACCOUNT_COLUMNS);
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            return ACCOUNT_MAPPER.queryOne(statement);
        }
    }

    @Override
    public boolean update(Account account) throws SQLException {
        String sql = """
                UPDATE users
                SET username = ?, role = ?, password_salt = ?, password_hash = ?
                WHERE id = ?
                """;
        try (var statement = database.getConnection().prepareStatement(sql)) {
            bindAccount(statement, account);
            statement.setLong(5, account.user().id());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        try (var statement = database.getConnection().prepareStatement("DELETE FROM users WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public Optional<Account> findByUsername(String username) throws SQLException {
        String sql = String.format("SELECT %s FROM users WHERE username = ?", ACCOUNT_COLUMNS);
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, username);
            return ACCOUNT_MAPPER.queryOne(statement);
        }
    }

    private static void bindAccount(PreparedStatement statement, Account account) throws SQLException {
        statement.setString(1, account.user().username());
        statement.setString(2, account.user().role().name());
        statement.setBytes(3, account.salt());
        statement.setBytes(4, account.passwordHash());
    }
}
