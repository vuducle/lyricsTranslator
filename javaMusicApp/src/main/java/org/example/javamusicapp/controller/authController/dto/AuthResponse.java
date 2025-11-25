package org.example.javamusicapp.controller.authController.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AuthResponse", description = "Response returned after successful authentication")
public class AuthResponse {
    @Schema(description = "Authenticated username", example = "julianguyen")
    private String username;

    @Schema(description = "User's email address", example = "julianguyen@example.com")
    private String email;

    @Schema(description = "User's full name", example = "Julian Nguyen")
    private String name;

    @Schema(description = "JWT access token", example = "eyJhbGci...", accessMode = Schema.AccessMode.READ_ONLY)
    private String accessToken;

    @Schema(description = "Refresh token stored server-side", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";
}
