package org.example.javamusicapp.handler.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.service.auth.AnmeldeversuchService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnmeldeversuchListener {

    private final AnmeldeversuchService anmeldeversuchService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String ip = "Unbekannt";
        if (event.getAuthentication().getDetails() instanceof WebAuthenticationDetails) {
            ip = ((WebAuthenticationDetails) event.getAuthentication().getDetails()).getRemoteAddress();
        }

        if (principal instanceof UserDetails) {
            User user = (User) principal;
            log.info("AUDIT: Erfolgreiche Anmeldung für Benutzer '{}' (E-Mail: {}) von IP-Adresse: {}", user.getUsername(), user.getEmail(), ip);
            anmeldeversuchService.anmeldungErfolgreich(user.getEmail());
        }
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String ip = "Unbekannt";
        if (event.getAuthentication().getDetails() instanceof WebAuthenticationDetails) {
            ip = ((WebAuthenticationDetails) event.getAuthentication().getDetails()).getRemoteAddress();
        }

        if (principal instanceof String) {
            String email = (String) principal;
            log.warn("AUDIT: Fehlgeschlagene Anmeldung für E-Mail '{}' von IP-Adresse: {}. Grund: {}", email, ip, event.getException().getMessage());
            anmeldeversuchService.anmeldungFehlgeschlagen(email);
        }
    }
}
