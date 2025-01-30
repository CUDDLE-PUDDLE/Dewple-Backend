package com.dewple.common.exception;

import com.dewple.common.response.status.ResponseStatus;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ResponseStatus responseStatus;

    public BaseException(ResponseStatus responseStatus) {
        super(responseStatus.getMessage());
        this.responseStatus = responseStatus;
    }

    public BaseException(ResponseStatus responseStatus, String overrideMessage) {
        super(overrideMessage);
        this.responseStatus = responseStatus;
    }

    public BaseException(ResponseStatus responseStatus, Throwable cause) {
        super(responseStatus.getMessage(), cause);
        this.responseStatus = responseStatus;
    }
}
