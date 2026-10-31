package com.cinema.exception;

import org.springframework.http.HttpStatus;

/** Signals the one refresh failure that must still commit session revocation. */
public final class InactiveAccountRefreshException extends BusinessException {

    public InactiveAccountRefreshException() {
        super(HttpStatus.UNAUTHORIZED, "Account is no longer active");
    }
}
