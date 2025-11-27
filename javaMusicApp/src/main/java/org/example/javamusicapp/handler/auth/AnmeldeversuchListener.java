package org.example.javamusicapp.handler.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.service.auth.AnmeldeversuchService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnmeldeversuchListener {

    private final AnmeldeversuchService anmeldeversuchService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            // Assuming the UserDetailsService loads user by username, but our app uses email for login.
            // We need to get the user object to get the email.
            anmeldeversuchService.getUserByEmail(((User) principal).getEmail()).ifPresent(user -> {
                log.info("Anmeldung erfolgreich für: {}", user.getEmail());
                anmeldeversuchService.anmeldungErfolgreich(user.getEmail());
            });
        }
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof String) {
            String email = (String) principal;
            log.warn("Fehlgeschlagene Anmeldung für: {}", email);
            anmeldeversuchService.anmeldungFehlgeschlagen(email);
        }
    }
}
