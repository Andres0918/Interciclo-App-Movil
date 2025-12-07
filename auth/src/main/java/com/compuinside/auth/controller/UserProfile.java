package com.compuinside.auth.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String uid;
    private String email;
    private String displayName;
    private String photoUrl;

    @Builder.Default
    private String role = "USER";

    private String accountId;

    @Builder.Default
    private Long createdAt = System.currentTimeMillis();

    @Builder.Default
    private Long lastLogin = System.currentTimeMillis();

    @Builder.Default
    private String state = "ACTIVE";
}
