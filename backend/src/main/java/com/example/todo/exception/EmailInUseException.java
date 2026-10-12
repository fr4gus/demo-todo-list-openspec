package com.example.todo.exception;

import org.springframework.http.HttpStatus;

public class EmailInUseException extends ApiException {

    public EmailInUseException() {
        super(HttpStatus.CONFLICT, "EMAIL_IN_USE", "Email is already in use.");
    }
}
