package com.dewple.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    int getCode();

    HttpStatus getHttpStatus();

    String getMessage();
}
