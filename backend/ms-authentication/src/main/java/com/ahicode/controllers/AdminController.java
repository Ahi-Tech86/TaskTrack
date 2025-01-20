package com.ahicode.controllers;

import com.ahicode.dtos.ConfirmationRegisterRequest;
import com.ahicode.dtos.UserDto;
import com.ahicode.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/admin")
public class AdminController {

    private final AuthService authService;

    @PostMapping("/confirmRegister")
    @Operation(
            summary = "Confirm and register admin",
            description = "Receiving confirm code for register, register user and create account",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successful registration"),
                    @ApiResponse(responseCode = "401", description = "Confirm code doesn't match with generated"),
                    @ApiResponse(responseCode = "500", description = "Error while serializing message")
            }
    )
    public ResponseEntity<UserDto> confirmAdminRegister(@RequestBody ConfirmationRegisterRequest confirmationRegisterRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.confirmAdmin(confirmationRegisterRequest));
    }
}
