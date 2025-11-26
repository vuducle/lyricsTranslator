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
import org.example.javamusicapp.service.nachweis.NachweisSecurityService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;

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
    private final NachweisSecurityService nachweisSecurityService;

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

    @Operation(summary = "Admin-Liste", description = "Gibt alle aktuellen Admin-User zurück")
    @GetMapping("/admins")
    @PreAuthorize("hasRole('ADMIN') or @nachweisSecurityService.isAusbilder(authentication)")
    public ResponseEntity<java.util.List<UserResponse>> listAdmins(Authentication authentication) {
        java.util.List<User> admins = userService.listAdmins();
        java.util.List<UserResponse> resp = admins.stream()
                .map(this::toUserResponse)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getAusbildungsjahr(),
                user.getTelefonnummer(),
                user.getTeam()
        );
    }

    @Operation(summary = "Profilbild hochladen", description = "Lädt ein Profilbild für den aktuell angemeldeten User hoch")
    @PutMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User updatedUser = userService.uploadProfileImage(username, file);

            UserResponse response = toUserResponse(updatedUser);

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

        UserResponse response = toUserResponse(user);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "User-Profil aktualisieren", description = "Aktualisiert das Profil des aktuell angemeldeten Users")
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateUserProfile(
            @RequestBody org.example.javamusicapp.controller.userController.dto.UserUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        User updatedUser = userService.updateUserProfile(username, request);
        UserResponse response = toUserResponse(updatedUser);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Profilbild löschen", description = "Löscht das Profilbild des aktuell angemeldeten Users")
    @DeleteMapping("/profile-image")
    public ResponseEntity<UserResponse> deleteProfileImage(Authentication authentication) {
        try {
            String username = authentication.getName();
            User updatedUser = userService.deleteProfileImage(username);

            UserResponse response = toUserResponse(updatedUser);

            log.info("Profilbild erfolgreich gelöscht für User: {}", username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Fehler beim Löschen des Profilbilds: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Admin-Rolle zuweisen", description = "Weist einem anderen Benutzer die ROLE_ADMIN zu. Nur für bestehende Admins oder Ausbilder.")
    @PutMapping("/{username}/grant-admin")
    @PreAuthorize("hasRole('ADMIN') or @nachweisSecurityService.isAusbilder(authentication)")
    public ResponseEntity<String> grantAdmin(@PathVariable("username") String username, Authentication authentication) {
        try {
            String caller = (authentication != null) ? authentication.getName() : "system";
            userService.grantAdminRoleToUser(username, caller);
            return ResponseEntity.ok("ROLE_ADMIN erfolgreich zugewiesen an " + username);
        } catch (IllegalArgumentException e) {
            log.warn("Grant admin fehlgeschlagen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Fehler beim Zuweisen von ROLE_ADMIN: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Zuweisen der Rolle");
        }
    }

    @Operation(summary = "Admin-Rolle entziehen", description = "Entzieht einem Benutzer die ROLE_ADMIN. Nur für bestehende Admins oder Ausbilder.")
    @DeleteMapping("/{username}/revoke-admin")
    @PreAuthorize("hasRole('ADMIN') or @nachweisSecurityService.isAusbilder(authentication)")
    public ResponseEntity<String> revokeAdmin(@PathVariable("username") String username,
            Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam(value = "keepAsNoRole", defaultValue = "false") boolean keepAsNoRole) {
        try {
            // Prevent self-revoke in specific cases:
            // - if caller is ADMIN -> prevent self-revoke
            // - if caller is Ausbilder (and not admin) -> also prevent self-revoke
            if (authentication != null && authentication.getName().equals(username)) {
                String caller = authentication.getName();
                boolean callerIsAdmin = userService.isAdmin(caller);
                boolean callerIsAusbilder = nachweisSecurityService.isAusbilder(authentication);

                if (callerIsAdmin || (callerIsAusbilder && !callerIsAdmin)) {
                    log.warn("Benutzer {} versucht, sich selbst die Admin-Rolle zu entziehen (verboten)", caller);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Entziehen nicht erlaubt: Sie können sich nicht selbst die Admin-Rolle entziehen.");
                }
                // otherwise allow (shouldn't normally happen because only admins/ausbilder can
                // call),
                // but we fall through to allow self-revoke for callers that are neither admin
                // nor ausbilder.
            }

            String caller = (authentication != null) ? authentication.getName() : "system";
            userService.revokeAdminRoleFromUser(username, caller, keepAsNoRole);
            return ResponseEntity.ok("ROLE_ADMIN erfolgreich entzogen von " + username);
        } catch (IllegalArgumentException e) {
            log.warn("Revoke admin fehlgeschlagen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            // z.B. letzter Admin
            log.warn("Revoke admin abgebrochen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Fehler beim Entziehen von ROLE_ADMIN: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Entfernen der Rolle");
        }
    }
}
