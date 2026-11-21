package org.campusfix.model.domain;

public record User(long id, String username, Role role) {
    public enum Role {
        REGULAR,
        ADMIN
    }
}
