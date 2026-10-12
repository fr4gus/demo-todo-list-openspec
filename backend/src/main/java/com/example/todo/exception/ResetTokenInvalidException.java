package com.example.todo.exception;

import org.springframework.http.HttpStatus;

/** Unknown, expired, already used or superseded: deliberately indistinguishable to the caller. */
public class ResetTokenInvalidException extends ApiException {

    public ResetTokenInvalidException() {
        super(HttpStatus.BAD_REQUEST, "RESET_TOKEN_INVALID", "This password reset link is invalid or has expired.");
    }
}
