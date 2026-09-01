package com.cleotaginador.login.exception;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cleotaginador.login.DTO.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalLoginException.class)
    public ResponseEntity<ErrorResponse> handleCustomGlobalLoginException(GlobalLoginException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getCodigo(), ex.getMessage(), getErrorTimestamp());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(RuntimeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(500, "Error interno en el servidor: " + ex.getMessage(), getErrorTimestamp());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getErrorTimestamp() {
        long timestamp = System.currentTimeMillis();
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDateTime fechaHora = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return fechaHora.format(formatter);
    }
}
