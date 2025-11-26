package org.example.javamusicapp.controller.authController.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "RegistrierungsAnfrage", description = "Anfrage-Payload zum Registrieren eines neuen Benutzers")
public class RegistrationRequest {
    @Schema(description = "Eindeutiger Benutzername des Benutzers", example = "julianguyen")
    private String username;

    @Schema(description = "Vollständiger Name des Benutzers", example = "Julian Nguyen")
    private String name;

    @Schema(description = "Benutzerpasswort", example = "password123", format = "password")
    private String password;

    @Schema(description = "E-Mail-Adresse des Benutzers", example = "julianguyen@example.com")
    private String email;

    @Schema(description = "Ausbildungsjahr des Azubis", example = "1")
    private Integer ausbildungsjahr;

    @Schema(description = "Telefonnummer des Benutzers", example = "0123456789")
    private String telefonnummer;

    @Schema(description = "Team des Benutzers", example = "Team A")
    private String team;
}
