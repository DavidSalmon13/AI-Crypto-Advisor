package com.moveo.aicryptoadvisor.exception;

import org.springframework.http.HttpStatus;

public class PreferencesNotSetException extends ApiException {

    public PreferencesNotSetException() {
        super(HttpStatus.NOT_FOUND, "PREFERENCES_NOT_SET", "Onboarding has not been completed yet.");
    }
}
