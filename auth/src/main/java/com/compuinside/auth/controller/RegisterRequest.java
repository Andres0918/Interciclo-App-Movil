package com.compuinside.auth.controller;

import com.compuinside.auth.dto.UserPlan;
import com.compuinside.auth.user.Empresa;
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
public class RegisterRequest {
    String username;
    String password;
    String email;
    ServiceClient serviceClient;
    String empresa;
    State state;
    Role role;
    UserPlan userPlan;
}
