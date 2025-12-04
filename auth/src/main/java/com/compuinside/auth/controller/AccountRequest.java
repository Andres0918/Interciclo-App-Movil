package com.compuinside.auth.controller;

import com.compuinside.auth.dto.UserPlan;
import com.compuinside.auth.user.Role;
import com.compuinside.auth.user.ServiceClient;
import com.compuinside.auth.user.State;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {
    String username;
    String password;
    String email;
    String accountId;
    ServiceClient serviceClient;
    String empresa;
    State state;
    Role role;
    UserPlan userPlan;
}
