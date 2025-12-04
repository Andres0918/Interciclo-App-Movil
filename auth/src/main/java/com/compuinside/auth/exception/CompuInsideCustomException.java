package com.compuinside.auth.exception;

import com.compuinside.auth.dto.Severity;
import lombok.Getter;

@Getter
public class CompuInsideCustomException extends RuntimeException {
    private final String title;
    private final Severity severity;

    public CompuInsideCustomException(String title, String message, Severity severity) {
        super(message);
        this.title = title;
        this.severity = severity;
    }
}
