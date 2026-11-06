package com.a9.arbor.grc.modules.iam.service;

/** Thrown on failed authentication (unknown user, wrong password, or disabled account). */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
