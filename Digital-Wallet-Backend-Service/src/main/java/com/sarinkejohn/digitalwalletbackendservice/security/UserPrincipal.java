package com.sarinkejohn.digitalwalletbackendservice.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class UserPrincipal implements Principal {
    private Long userId;
    private String username;
    private String role;

    @Override
    public String getName() {
        return username;
    }
}