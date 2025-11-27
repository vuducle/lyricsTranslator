package org.example.javamusicapp.config.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 🔒 **Was geht hier ab?**
 * Diese Klasse ist der Bodyguard für unsere User-Passwörter. Sie stellt eine `@Bean` vom `PasswordEncoder`
 * bereit, die dann in der ganzen App per Dependency Injection genutzt werden kann.
 *
 * Konkret nutzt sie `BCryptPasswordEncoder`, einen Standard-Algorithmus, um Passwörter sicher zu hashen.
 * Das heißt, Passwörter werden niemals im Klartext in der Datenbank gespeichert, sondern nur als
 * unlesbarer Zeichensalat.
 *
 * Wenn ein User sich einloggt, wird sein eingegebenes Passwort wieder mit BCrypt gehasht und dann mit dem
 * Hash in der DB verglichen. Maximale Security, so muss das sein.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
