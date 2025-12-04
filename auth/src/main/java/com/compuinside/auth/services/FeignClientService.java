package com.compuinside.auth.services;

import com.compuinside.auth.dto.UserEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "doctor-module")
public interface FeignClientService {

    @PostMapping("/med/sync-user")
    void syncUser(@RequestBody UserEvent userEvent);

}
