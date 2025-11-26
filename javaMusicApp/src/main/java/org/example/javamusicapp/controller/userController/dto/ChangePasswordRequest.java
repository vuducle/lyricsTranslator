package org.example.javamusicapp.controller.userController.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Altes Passwort ist erforderlich")
    private String oldPassword;

    @NotBlank(message = "Neues Passwort ist erforderlich")
    @Size(min = 6, message = "Passwort muss mindestens 6 Zeichen lang sein")
    private String newPassword;
}
