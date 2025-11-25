package org.example.javamusicapp.controller.authController.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "RegistrationRequest", description = "Request payload for registering a new user")
public class RegistrationRequest {
    @Schema(description = "Unique username of the user", example = "julianguyen")
    private String username;

    @Schema(description = "User's full name", example = "Julian Nguyen")
    private String name;

    @Schema(description = "User password", example = "password123", format = "password")
    private String password;

    @Schema(description = "User email address", example = "julianguyen@example.com")
    private String email;
}
