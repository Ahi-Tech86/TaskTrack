package com.ahicode.services;

import com.ahicode.dtos.*;

import java.util.List;

public interface AuthService {
    String register(SignUpRequest signUpRequest);
    UserDto confirm(ConfirmationRegisterRequest confirmRegisterRequest);
    UserDto confirmAdmin(ConfirmationRegisterRequest confirmationRegisterRequest);
    AuthResponse login(SignInRequest signInRequest);
}
