package com.compuinside.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorDTO {
    private final String title;
    private final String message;
    private final Severity severity;
}
