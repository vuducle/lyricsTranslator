package org.example.javamusicapp.service.nachweis;

import lombok.RequiredArgsConstructor;
import org.example.javamusicapp.repository.NachweisRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 🛡️ **Was geht hier ab?**
 * Dieser Service ist unser Custom Security Dude für die `Nachweis`-Controller.
 * Er stellt spezielle Methoden bereit, die wir direkt in den `@PreAuthorize`-Annotationen
 * verwenden können, um komplexe Berechtigungen zu checken, die über einfache Rollen
 * hinausgehen.
 *
 * Die Skills:
 * - **isOwner()**: Checkt, ob der aktuell eingeloggte User auch wirklich der Owner
 *   (also der Ersteller) von einem bestimmten Nachweis ist. Damit stellen wir sicher,
 *   dass User A nicht die Nachweise von User B bearbeiten kann.
 * - **isAusbilder()**: Checkt, ob ein User ein "Ausbilder" ist. Diese Info steht nicht
 *   direkt in den User-Rollen, sondern wird daraus abgeleitet, ob der User in der
 *   `Nachweis`-Tabelle als Ausbilder eingetragen ist.
 *
 * Macht unsere Security-Regeln also flexibler und smarter.
 */
@Service("nachweisSecurityService")
@RequiredArgsConstructor
public class NachweisSecurityService {

    private final NachweisRepository nachweisRepository;

    public boolean isOwner(Authentication authentication, UUID nachweisId) {
        String username = authentication.getName();
        return nachweisRepository.findById(nachweisId)
                .map(nachweis -> nachweis.getAzubi().getUsername().equals(username))
                .orElse(false);
    }

    /*
     * Überprüft, ob der aktuell authentifizierte Benutzer ein Ausbilder ist.
     * Ein Benutzer gilt als Ausbilder, wenn sein Benutzername in der
     * Nachweis-Tabelle
     * als Ausbilder-Username existiert.
     */
    public boolean isAusbilder(Authentication authentication) {
        if (authentication == null)
            return false;
        String username = authentication.getName();
        try {
            return nachweisRepository.existsByAusbilderUsername(username);
        } catch (Exception e) {
            return false;
        }
    }
}
