package com.pageon.backend.exception;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
public class ErrorResponse {

    private final String errorCode;
    private final String errorMessage;
    private final LocalDateTime time;

}
