package com.ahicode.enums;

import org.springframework.security.core.GrantedAuthority;

public enum AppRole implements GrantedAuthority {
    USER,
    ADMIN;

    @Override
    public String getAuthority() {
        return name();
    }
}
