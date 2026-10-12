package com.example.todo.exception;

import org.springframework.http.HttpStatus;

public class EmailNotFoundException extends ApiException {

    public EmailNotFoundException() {
        super(HttpStatus.UNAUTHORIZED, "EMAIL_NOT_FOUND", "Email doesn't exist");
    }
}
