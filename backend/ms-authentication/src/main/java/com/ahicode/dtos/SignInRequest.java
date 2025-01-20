package com.ahicode.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignInRequest {
    @Schema(description = "User's email", example = "example@mail.com")
    private String email;
    @Schema(description = "User's password", example = "password")
    private String password;
}
