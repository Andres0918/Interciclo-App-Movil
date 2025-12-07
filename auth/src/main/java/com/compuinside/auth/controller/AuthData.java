package com.compuinside.auth.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthData {
    private String uid;
    private String email;
    private String displayName;
    private String photoUrl;
    private String customToken;
    private String role;
    private String accountId;
}
