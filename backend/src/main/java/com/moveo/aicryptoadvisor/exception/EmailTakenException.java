package com.moveo.aicryptoadvisor.exception;

import org.springframework.http.HttpStatus;

public class EmailTakenException extends ApiException {

    public EmailTakenException(String email) {
        super(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email '" + email + "' is already registered.");
    }
}
