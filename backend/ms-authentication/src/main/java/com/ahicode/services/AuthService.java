package com.ahicode.services;

import com.ahicode.dtos.ConfirmationRegisterRequest;
import com.ahicode.dtos.SignInRequest;
import com.ahicode.dtos.SignUpRequest;
import com.ahicode.dtos.UserDto;

import java.util.List;

public interface AuthService {
    String register(SignUpRequest signUpRequest);
    UserDto confirm(ConfirmationRegisterRequest confirmRegisterRequest);
    UserDto confirmAdmin(ConfirmationRegisterRequest confirmationRegisterRequest);
    List<Object> login(SignInRequest signInRequest);
}
