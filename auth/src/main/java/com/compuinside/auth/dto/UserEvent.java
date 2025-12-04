package com.compuinside.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserEvent {
    private UUID userId;
    private String role;
    private String serviceClient;
    private String userPlan;
    private String action;
}
