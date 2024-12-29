package com.example.commonsetting.global.exception.code;

import org.springframework.http.HttpStatus;

public interface ResponseCode {
    String getCode();
    HttpStatus getHttpStatus();
}
