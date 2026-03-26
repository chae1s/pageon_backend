package com.pageon.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        log.error("CustomException 발생: ErrorCode = {}, ErrorMessage = {}", exception.getErrorCode(), exception.getErrorMessage());

        ErrorResponse errorResponse = new ErrorResponse(exception.getErrorCode(), exception.getErrorMessage(), LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, exception.getHttpStatus());

    }


}
