package org.example.javamusicapp.controller.userController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.controller.userController.dto.ChangePasswordRequest;
import org.example.javamusicapp.controller.userController.dto.UserResponse;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.service.auth.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller für User-bezogene Endpunkte in der JavaMusicApp.
 * Bietet Funktionen zum Ändern des Passworts, Hochladen von Profilbildern und
 * Abrufen von User-Profilinformationen.
 * Autor: Vu Minh Le
 * Version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User-Profil-Verwaltung")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Passwort ändern", description = "Ändert das Passwort des aktuell angemeldeten Users")
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            userService.changePassword(username, request.getOldPassword(), request.getNewPassword());
            log.info("Passwort erfolgreich geändert für User: {}", username);
            return ResponseEntity.ok("Passwort erfolgreich geändert");
        } catch (IllegalArgumentException e) {
            log.warn("Passwortänderung fehlgeschlagen: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Profilbild hochladen", description = "Lädt ein Profilbild für den aktuell angemeldeten User hoch")
    @PutMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User updatedUser = userService.uploadProfileImage(username, file);

            UserResponse response = new UserResponse(
                    updatedUser.getId(),
                    updatedUser.getUsername(),
                    updatedUser.getName(),
                    updatedUser.getEmail(),
                    updatedUser.getProfileImageUrl());

            log.info("Profilbild erfolgreich hochgeladen für User: {}", username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Fehler beim Hochladen des Profilbilds: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "User-Profil abrufen", description = "Gibt das Profil des aktuell angemeldeten Users zurück")
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserProfile(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        UserResponse response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getProfileImageUrl());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Profilbild löschen", description = "Löscht das Profilbild des aktuell angemeldeten Users")
    @DeleteMapping("/profile-image")
    public ResponseEntity<UserResponse> deleteProfileImage(Authentication authentication) {
        try {
            String username = authentication.getName();
            User updatedUser = userService.deleteProfileImage(username);

            UserResponse response = new UserResponse(
                    updatedUser.getId(),
                    updatedUser.getUsername(),
                    updatedUser.getName(),
                    updatedUser.getEmail(),
                    updatedUser.getProfileImageUrl());

            log.info("Profilbild erfolgreich gelöscht für User: {}", username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Fehler beim Löschen des Profilbilds: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
