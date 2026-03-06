package com.briefix.auth.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String email) {
        super("Email address is not verified: " + email);
    }
}
