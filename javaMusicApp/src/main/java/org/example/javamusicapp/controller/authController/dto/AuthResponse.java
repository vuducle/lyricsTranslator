package org.example.javamusicapp.controller.authController.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AuthAntwort", description = "Antwort, die nach erfolgreicher Authentifizierung zurückgegeben wird")
public class AuthResponse {
    @Schema(description = "Authentifizierter Benutzername", example = "julianguyen")
    private String username;

    @Schema(description = "E-Mail-Adresse des Benutzers", example = "julianguyen@example.com")
    private String email;

    @Schema(description = "Vollständiger Name des Benutzers", example = "Julian Nguyen")
    private String name;

    @Schema(description = "Rollen des Benutzers", example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
    private java.util.List<String> roles;

    @Schema(description = "JWT-Zugriffstoken", example = "eyJhbGci...", accessMode = Schema.AccessMode.READ_ONLY)
    private String accessToken;

    @Schema(description = "Serverseitig gespeicherter Refresh-Token", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String refreshToken;

    @Schema(description = "Tokentyp", example = "Bearer")
    private String tokenType = "Bearer";
}
