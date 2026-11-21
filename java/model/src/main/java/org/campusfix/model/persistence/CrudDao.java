package org.campusfix.model.persistence;

import java.sql.SQLException;
import java.util.Optional;

public interface CrudDao<T> {
    /** Inserts an entity and returns it with its generated ID. */
    T create(T entity) throws SQLException;

    /** Returns an empty result when the ID does not exist. */
    Optional<T> read(long id) throws SQLException;

    /** Updates the entity by its ID and returns whether it was found. */
    boolean update(T entity) throws SQLException;

    /** Deletes the entity by ID and returns whether it was found. */
    boolean delete(long id) throws SQLException;
}
