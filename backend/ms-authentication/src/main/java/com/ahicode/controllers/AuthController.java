package com.ahicode.controllers;

import com.ahicode.dtos.*;
import com.ahicode.services.AuthService;
import com.ahicode.services.MessageProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final MessageProducerService messageService;

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

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
                    @ApiResponse(responseCode = "400", description = "Operation has been blocked"),
                    @ApiResponse(responseCode = "401", description = "Password doesn't match with user password"),
                    @ApiResponse(responseCode = "404", description = "User with email doesn't exists")
            }
    )
    public ResponseEntity<UserDto> authenticateUser(HttpServletResponse response, @RequestBody SignInRequest signInRequest) {
        AuthResponse authenticatedUser = authService.login(signInRequest);

        UserDto userDto = authenticatedUser.getUserDto();
        String accessToken = authenticatedUser.getAccessToken();
        String refreshToken = authenticatedUser.getRefreshToken();

        Cookie accessTokenCookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(60 * 60);

        Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
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
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractCookieValue(ACCESS_TOKEN_COOKIE_NAME, request);
        String refreshToken = extractCookieValue(REFRESH_TOKEN_COOKIE_NAME, request);

        if (accessToken != null && refreshToken != null) {
            messageService.sendMessage(List.of(accessToken, refreshToken));
        } else {
            log.error("Access token or refresh token not found in cookies");
        }

        deleteCookie(ACCESS_TOKEN_COOKIE_NAME, response);
        deleteCookie(REFRESH_TOKEN_COOKIE_NAME, response);

        return ResponseEntity.noContent().build();
    }

    private void deleteCookie(String cookieName, HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private String extractCookieValue(String cookieName, HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);

        if (cookie != null) {
            return cookie.getValue();
        } else {
            log.warn("Cookie {} not found in request", cookieName);
            return null;
        }
    }
}
