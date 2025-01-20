package com.ahicode.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is mandatory")
    @Size(min = 5, max = 50, message = "Email must be between 5 and 50 characters")
    @Schema(description = "User's email", example = "example@mail.com")
    private String email;

    @NotBlank(message = "Nickname is mandatory")
    @Size(min = 5, max = 50, message = "Nickname must be between 5 and 50 characters")
    @Schema(description = "User nickname in the application", example = "UserNickname007")
    private String nickname;

    @NotBlank(message = "Firstname is mandatory")
    @Size(min = 3, max = 35, message = "Firstname must be between 3 and 35 characters")
    @Schema(description = "User's firstname", example = "Firstname")
    private String firstname;

    @NotBlank(message = "Lastname is mandatory")
    @Size(min = 3, max = 35, message = "Lastname must be between 3 and 35 characters")
    @Schema(description = "User's firstname", example = "Lastname")
    private String lastname;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 1, max = 100, message = "Password must be between 1 and 100 characters")
    @Schema(description = "User's password", example = "password")
    private String password;
}
