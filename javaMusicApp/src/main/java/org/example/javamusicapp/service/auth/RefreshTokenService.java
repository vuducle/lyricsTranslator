package org.example.javamusicapp.service.auth;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.handler.TokenRefreshException;
import org.example.javamusicapp.model.RefreshToken;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.repository.RefreshTokenRepository;
import org.example.javamusicapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenService {
    @Value("${jwt.refresh.expiration.days}")
    private long refreshExpirationDays;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Erstellt einen neuen Refresh Token für den Benutzer.
     * 
     * @param user Der Benutzer, für den der Token erstellt wird.
     * @return Das erstellte RefreshToken-Objekt.
     */
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.findByUserId(user.getId())
                .ifPresent(refreshTokenRepository::delete);

        // BERECHNE DIE DAUER ALS EINEN LANGEN WERT (30 Tage * 24h * 60min * 60sek)
        long durationSeconds = refreshExpirationDays * 24 * 60 * 60;

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID().toString())
                // WICHTIG: Verwende den fertigen Wert in plusSeconds()
                .expiryDate(Instant.now().plusSeconds(durationSeconds))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Sucht den Refresh Token in der Datenbank.
     * 
     * @param token Der String des Refreshtokens.
     * @return Optional<RefreshToken>
     */
    public Optional<RefreshToken> getRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Überprüft, ob der Token abgelaufen ist. Wenn ja, löscht er ihn.
     * 
     * @param token Der zu prüfende RefreshToken.
     * @return Den RefreshToken, wenn er gültig ist.
     * @throws TokenRefreshException Wenn der Token abgelaufen ist.
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh Token ist abgelaufen. Bitte melden Sie sich erneut an.");
        }
        return token;
    }

    /**
     * Löscht den Refresh Token, z.B. beim Logout.
     */
    @Transactional
    public void deleteByUserId(UUID userId) {
        refreshTokenRepository.findByUserId(userId)
                .ifPresent(refreshTokenRepository::delete);
    }

    /**
     * Sucht den Refresh Token in der Datenbank.
     * 
     * @param token Der String des Refreshtokens.
     * @return Optional<RefreshToken>
     */
    public Optional<RefreshToken> findByToken(String token) {
        log.info("=== Finding refresh token: {}", token);
        Optional<RefreshToken> result = refreshTokenRepository.findByToken(token);
        log.info("=== Token found: {}", result.isPresent());
        return result;
    }
}
