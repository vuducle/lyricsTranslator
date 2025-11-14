package org.example.javamusicapp.service;

import jakarta.transaction.Transactional;
import org.example.javamusicapp.controller.authController.dto.AuthResponse;
import org.example.javamusicapp.handler.TokenRefreshException;
import org.example.javamusicapp.model.RefreshToken;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.repository.RefreshTokenRepository;
import org.example.javamusicapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${jwt.refresh.expiration.days}")
    private int refreshExpirationDays;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Erstellt einen neuen Refresh Token für den Benutzer.
     * @param user Der Benutzer, für den der Token erstellt wird.
     * @return Das erstellte RefreshToken-Objekt.
     */
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusSeconds((long) refreshExpirationDays * 24 * 60 * 60);

        RefreshToken refreshToken = RefreshToken.builder().user(user).token(token).expiryDate(expiryDate).build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Sucht den Refresh Token in der Datenbank.
     * @param token Der String des Refreshtokens.
     * @return Optional<RefreshToken>
     */
    public Optional<RefreshToken> getRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Überprüft, ob der Token abgelaufen ist. Wenn ja, löscht er ihn.
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
    public int deleteByUserId(Long userId) {
        return 0;
    }

    /**
     * Sucht den Refresh Token in der Datenbank.
     * @param token Der String des Refreshtokens.
     * @return Optional<RefreshToken>
     */
    public Optional<RefreshToken> findByToken(String token) {
        // Delegation an das Repository
        return refreshTokenRepository.findByToken(token);
    }
}
