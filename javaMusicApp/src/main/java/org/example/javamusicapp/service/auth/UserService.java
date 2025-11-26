package org.example.javamusicapp.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String UPLOAD_DIR = "uploads/profile-images/";

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        // Erstelle das Upload-Verzeichnis, falls es nicht existiert
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            log.error("Fehler beim Erstellen des Upload-Verzeichnisses: {}", e.getMessage());
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = findByUsername(username);

        // Überprüfe, ob das alte Passwort korrekt ist
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Altes Passwort ist nicht korrekt");
        }

        // Setze das neue Passwort
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Passwort geändert für User: {}", username);
    }

    public User uploadProfileImage(String username, MultipartFile file) throws IOException {
        User user = findByUsername(username);

        // Validiere die Datei
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Datei ist leer");
        }

        // Validiere den Dateityp
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Nur Bilddateien sind erlaubt");
        }

        // Generiere einen eindeutigen Dateinamen
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String newFilename = user.getId() + "_" + UUID.randomUUID() + fileExtension;

        // Speichere die Datei
        Path targetPath = Paths.get(UPLOAD_DIR + newFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Lösche das alte Bild, falls vorhanden
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            try {
                String oldFilename = user.getProfileImageUrl()
                        .substring(user.getProfileImageUrl().lastIndexOf("/") + 1);
                Path oldPath = Paths.get(UPLOAD_DIR + oldFilename);
                Files.deleteIfExists(oldPath);
            } catch (Exception e) {
                log.warn("Fehler beim Löschen des alten Profilbilds: {}", e.getMessage());
            }
        }

        // Update den User
        user.setProfileImageUrl("/uploads/profile-images/" + newFilename);
        User savedUser = userRepository.save(user);
        log.info("Profilbild hochgeladen für User: {}", username);

        return savedUser;
    }

    public User deleteProfileImage(String username) throws IOException {
        User user = findByUsername(username);

        // Lösche das Bild aus dem Dateisystem, falls vorhanden
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            try {
                String filename = user.getProfileImageUrl()
                        .substring(user.getProfileImageUrl().lastIndexOf("/") + 1);
                Path filePath = Paths.get(UPLOAD_DIR + filename);
                Files.deleteIfExists(filePath);
                log.info("Profilbild-Datei gelöscht: {}", filename);
            } catch (Exception e) {
                log.error("Fehler beim Löschen der Profilbild-Datei: {}", e.getMessage());
                throw new IOException("Fehler beim Löschen der Profilbild-Datei", e);
            }
        }

        // Setze die URL auf null
        user.setProfileImageUrl(null);
        User savedUser = userRepository.save(user);
        log.info("Profilbild-URL entfernt für User: {}", username);

        return savedUser;
    }
}
