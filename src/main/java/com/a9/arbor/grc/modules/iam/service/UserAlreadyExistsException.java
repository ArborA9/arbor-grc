package com.a9.arbor.grc.modules.iam.service;

/** Thrown when creating a user whose email already exists in the tenant. */
public class UserAlreadyExistsException extends RuntimeException {

    private final String email;

    public UserAlreadyExistsException(String email) {
        super("A user with email \"" + email + "\" already exists");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
