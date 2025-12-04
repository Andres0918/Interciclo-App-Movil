package com.compuinside.auth.exception;

import com.compuinside.auth.dto.ErrorDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CompuInsideCustomException.class)
    public ResponseEntity<ErrorDTO> handleCustomException(CompuInsideCustomException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .title(ex.getTitle())
                .message(ex.getMessage())
                .severity(ex.getSeverity())
                .build();

        return ResponseEntity.badRequest().body(error);
    }
}
