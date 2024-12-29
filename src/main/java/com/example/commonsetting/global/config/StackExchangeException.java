package com.example.commonsetting.global.config;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StackExchangeException extends RuntimeException {

    private final String code;
    private HttpStatus httpStatus;

    public StackExchangeException(String code) {
        super();
        this.code = code;
    }

    public StackExchangeException(HttpStatus httpStatus, String code, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public StackExchangeException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public StackExchangeException(String code, String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.code = code;
    }


    public StackExchangeException(HttpStatus httpStatus, String message) {
        super(message);
        this.code = String.valueOf(400);;
        this.httpStatus = httpStatus;
    }
}
