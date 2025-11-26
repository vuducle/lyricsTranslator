package org.example.javamusicapp.service.auth;

import org.example.javamusicapp.handler.TokenRefreshException;
import org.example.javamusicapp.model.PasswordResetToken;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenService {

    private final Long passwordResetTokenExpirationMs = 3600000L; // 1 hour

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        return passwordResetTokenRepository.findByToken(token);
    }

    public PasswordResetToken createPasswordResetToken(User user) {
        // Invalidate previous tokens for the same user
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setUser(user);
        passwordResetToken.setExpiryDate(Instant.now().plusMillis(passwordResetTokenExpirationMs));
        passwordResetToken.setToken(UUID.randomUUID().toString());

        return passwordResetTokenRepository.save(passwordResetToken);
    }

    public PasswordResetToken verifyExpiration(PasswordResetToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Password reset token was expired. Please make a new request.");
        }
        return token;
    }

    @Transactional
    public void deleteToken(String token) {
        passwordResetTokenRepository.findByToken(token).ifPresent(passwordResetTokenRepository::delete);
    }
}
