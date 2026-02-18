package com.example.demo.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ DataAccessException.class, CannotGetJdbcConnectionException.class })
    public ResponseEntity<Map<String, Object>> handleDatabaseUnavailable(Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "DB_UNAVAILABLE",
                "message", "Database is temporarily unavailable",
                "timestamp", Instant.now().toString()
        ));
    }
}
