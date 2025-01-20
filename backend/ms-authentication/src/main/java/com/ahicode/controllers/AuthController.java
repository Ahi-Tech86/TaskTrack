package com.ahicode.controllers;

import com.ahicode.dtos.ConfirmationRegisterRequest;
import com.ahicode.dtos.SignInRequest;
import com.ahicode.dtos.SignUpRequest;
import com.ahicode.dtos.UserDto;
import com.ahicode.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Sending activation code on email",
            description = "Sending data for register and saving them in db, then generate activation code and send on email",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Activation code was send on email"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "500", description = "An error occurred while sending message an email")
            }
    )
    public ResponseEntity<String> register(@Valid @RequestBody SignUpRequest signUpRequest) {
        return ResponseEntity.ok(authService.register(signUpRequest));
    }

    @PostMapping("/confirmRegister")
    @Operation(
            summary = "Confirm and register user",
            description = "Receiving confirm code for register, register user and create account",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successful registration"),
                    @ApiResponse(responseCode = "401", description = "Confirm code doesn't match with generated"),
                    @ApiResponse(responseCode = "500", description = "Error while serializing message")
            }
    )
    public ResponseEntity<UserDto> confirmRegister(@RequestBody ConfirmationRegisterRequest confirmationRegisterRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.confirm(confirmationRegisterRequest));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login operation",
            description = "Login operation and JWT tokens saving",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful login operation"),
                    @ApiResponse(responseCode = "401", description = "Password doesn't match with user password"),
                    @ApiResponse(responseCode = "404", description = "User with email doesn't exists")
            }
    )
    public ResponseEntity<UserDto> authenticateUser(HttpServletResponse response, @RequestBody SignInRequest signInRequest) {
        List<Object> authenticatedUser = authService.login(signInRequest);

        UserDto userDto = (UserDto) authenticatedUser.get(0);
        String accessToken = (String) authenticatedUser.get(1);
        String refreshToken = (String) authenticatedUser.get(2);

        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(60 * 60);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout operation",
            description = "Logout and clear all cookies",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successful logout")
            }
    )
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        deleteCookie("accessToken", response);
        deleteCookie("refreshToken", response);

        return ResponseEntity.noContent().build();
    }

    private void deleteCookie(String cookieName, HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
