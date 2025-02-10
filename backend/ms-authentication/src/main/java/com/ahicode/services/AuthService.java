package com.ahicode.services;

import com.ahicode.dtos.*;

public interface AuthService {
    String register(SignUpRequest signUpRequest);
    UserDto confirm(ConfirmationRegisterRequest confirmRegisterRequest);
    AuthResponse login(SignInRequest signInRequest);
}
