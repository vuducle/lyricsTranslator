package org.example.javamusicapp.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 🚨 **Was geht hier ab?**
 * Dieser Service ist unser Security Guard gegen Brute-Force-Attacken beim Login.
 * Er zählt mit, wie oft jemand versucht, sich mit falschen Credentials einzuloggen.
 *
 * So funktioniert's:
 * - **anmeldungFehlgeschlagen()**: Jedes Mal, wenn ein Login failt, zählt der Counter für die
 *   E-Mail-Adresse hoch. Nach zu vielen Fehlversuchen (z.B. 5) wird der Account für eine
 *   bestimmte Zeit (z.B. 15 Minuten) gesperrt. Sheesh!
 * - **anmeldungErfolgreich()**: Wenn der Login erfolgreich war, wird der Counter wieder auf 0
 *   gesetzt. Alles wieder fresh.
 * - **istGesperrt()**: Checkt, ob ein Account gerade eine Zwangspause einlegen muss.
 *
 * Das macht es für Hacker ultra nervig, einfach Tausende Passwörter durchzuprobieren.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnmeldeversuchService {

    private static final int MAX_FEHLVERSUCHE = 5;
    private static final int SPERRDAUER_MINUTEN = 15;

    private final UserRepository userRepository;

    public void anmeldungErfolgreich(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFehlgeschlageneAnmeldeversuche(0);
            user.setAccountGesperrtBis(null);
            userRepository.save(user);
        });
    }

    public void anmeldungFehlgeschlagen(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            int versuche = user.getFehlgeschlageneAnmeldeversuche() + 1;
            user.setFehlgeschlageneAnmeldeversuche(versuche);
            if (versuche >= MAX_FEHLVERSUCHE) {
                user.setAccountGesperrtBis(LocalDateTime.now().plusMinutes(SPERRDAUER_MINUTEN));
                log.warn("AUDIT: Account für E-Mail '{}' wurde für {} Minuten gesperrt.", email, SPERRDAUER_MINUTEN);
            }
            userRepository.save(user);
        });
    }

    public boolean istGesperrt(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return user.getAccountGesperrtBis() != null && user.getAccountGesperrtBis().isAfter(LocalDateTime.now());
        }
        return false;
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
