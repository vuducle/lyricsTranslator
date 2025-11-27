package org.example.javamusicapp.service.auth;

import lombok.RequiredArgsConstructor;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
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
