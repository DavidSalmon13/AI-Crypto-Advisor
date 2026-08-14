package com.moveo.aicryptoadvisor.exception;

import org.springframework.http.HttpStatus;

public class UnknownCoinIdException extends ApiException {

    public UnknownCoinIdException(String coinId) {
        super(HttpStatus.BAD_REQUEST, "UNKNOWN_COIN_ID",
                "Coin id '" + coinId + "' is not in the supported list.");
    }
}
