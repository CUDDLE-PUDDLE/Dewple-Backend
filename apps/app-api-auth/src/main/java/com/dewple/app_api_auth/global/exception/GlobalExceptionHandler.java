package com.dewple.app_api_auth.global.exception;

import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.common.exception.BusinessException;
import com.dewple.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[BusinessException] code={}, message={}", errorCode.getCode(), e.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();

        log.warn("[ValidationException] message={}", errorMessage);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        WebErrorCode.BAD_REQUEST.getCode(),
                        WebErrorCode.BAD_REQUEST.getHttpStatus().value(),
                        errorMessage
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[HttpMessageNotReadableException] message={}", e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(WebErrorCode.HTTP_MESSAGE_NOT_READABLE));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[HttpRequestMethodNotSupportedException] method={}", e.getMethod());

        return ResponseEntity
                .status(WebErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(ApiResponse.error(WebErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("[NoHandlerFoundException] url={}", e.getRequestURL());

        return ResponseEntity
                .status(WebErrorCode.URL_NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(WebErrorCode.URL_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception e) {
        log.error("[UnknownException] ", e);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(WebErrorCode.SERVER_ERROR));
    }
}
