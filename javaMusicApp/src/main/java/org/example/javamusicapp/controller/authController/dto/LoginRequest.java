package org.example.javamusicapp.controller.authController.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "LoginRequest", description = "Request payload for registering or logging in a user")
public class LoginRequest {
    @Schema(description = "Unique username of the user", example = "julianguyen")
    private String username;

    @Schema(description = "User's full name", example = "Julian Nguyen")
    private String name;

    @Schema(description = "User password", example = "password123", format = "password")
    private String password;

    @Schema(description = "User email address", example = "julianguyen@example.com")
    private String email;
}
