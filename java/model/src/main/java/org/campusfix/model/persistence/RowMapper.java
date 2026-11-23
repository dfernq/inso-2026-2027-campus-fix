package org.campusfix.model.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

abstract class RowMapper<T> {
    final Optional<T> queryOne(PreparedStatement statement) throws SQLException {
        try (var rows = statement.executeQuery()) {
            return rows.next() ? Optional.of(mapRow(rows)) : Optional.empty();
        }
    }

    final List<T> queryAll(PreparedStatement statement) throws SQLException {
        try (var rows = statement.executeQuery()) {
            var results = new ArrayList<T>();
            while (rows.next()) {
                results.add(mapRow(rows));
            }
            return results;
        }
    }

    abstract T mapRow(ResultSet row) throws SQLException;
}
