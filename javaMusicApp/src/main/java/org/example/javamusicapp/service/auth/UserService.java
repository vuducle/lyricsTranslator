package org.example.javamusicapp.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.service.audit.RoleAuditService;
import org.example.javamusicapp.repository.UserRepository;
import org.example.javamusicapp.repository.RoleRepository;
import org.example.javamusicapp.model.Role;
import org.example.javamusicapp.model.enums.ERole;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final RoleAuditService roleAuditService;
    private static final String UPLOAD_DIR = "uploads/profile-images/";
    @Value("${image.max-width:1024}")
    private int maxWidth;

    @Value("${image.max-height:1024}")
    private int maxHeight;

    @Value("${image.quality:0.75}")
    private float imageQuality;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository,
            RoleAuditService roleAuditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.roleAuditService = roleAuditService;
        // Erstelle das Upload-Verzeichnis, falls es nicht existiert
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            log.error("Fehler beim Erstellen des Upload-Verzeichnisses: {}", e.getMessage());
        }
    }

    public void grantAdminRoleToUser(String targetUsername) {
        grantAdminRoleToUser(targetUsername, "system");
    }

    public void grantAdminRoleToUser(String targetUsername, String performedBy) {
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("Zielbenutzer nicht gefunden: " + targetUsername));

        // Prüfe, ob Ziel bereits Admin ist
        boolean alreadyAdmin = target.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_ADMIN);
        if (alreadyAdmin) {
            log.info("Benutzer {} hat bereits ROLE_ADMIN", targetUsername);
            return;
        }

        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN ist nicht konfiguriert"));

        target.getRoles().add(adminRole);
        userRepository.save(target);
        log.info("ROLE_ADMIN zugewiesen an User: {}", targetUsername);
        try {
            roleAuditService.record("GRANT", targetUsername, performedBy, "Assigned ROLE_ADMIN");
        } catch (Exception e) {
            log.warn("Audit-Eintrag konnte nicht geschrieben werden: {}", e.getMessage());
        }
    }

    public void revokeAdminRoleFromUser(String targetUsername) {
        revokeAdminRoleFromUser(targetUsername, "system");
    }

    public void revokeAdminRoleFromUser(String targetUsername, String performedBy) {
        // backwards-compatible: keepAsNoRole = false (assign ROLE_USER if missing)
        revokeAdminRoleFromUser(targetUsername, performedBy, false);
    }

    /**
     * Revoke ROLE_ADMIN from a user.
     * 
     * @param targetUsername target user
     * @param performedBy    who performed the action (for audit)
     * @param keepAsNoRole   if true, do not auto-assign ROLE_USER after revoke; if
     *                       false, assign ROLE_USER when missing
     */
    public void revokeAdminRoleFromUser(String targetUsername, String performedBy, boolean keepAsNoRole) {
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("Zielbenutzer nicht gefunden: " + targetUsername));

        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN ist nicht konfiguriert"));

        boolean hasAdmin = target.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN);
        if (!hasAdmin) {
            log.info("User {} hatte ROLE_ADMIN nicht", targetUsername);
            return;
        }

        long adminCount = userRepository.countByRoles_Name(ERole.ROLE_ADMIN);
        if (adminCount <= 1) {
            // Prevent removing the last remaining admin
            throw new IllegalStateException("Entfernen nicht möglich: Es muss mindestens ein Admin-Account bestehen");
        }

        boolean removed = target.getRoles().removeIf(r -> r.getName() == ERole.ROLE_ADMIN);
        if (removed) {
            userRepository.save(target);
            log.info("ROLE_ADMIN entfernt von User: {}", targetUsername);
            try {
                roleAuditService.record("REVOKE", targetUsername, performedBy, "Removed ROLE_ADMIN");
            } catch (Exception e) {
                log.warn("Audit-Eintrag konnte nicht geschrieben werden: {}", e.getMessage());
            }

            if (!keepAsNoRole) {
                // Wenn der User danach keine ROLE_USER hat, setzen wir ihn als Azubi
                // (ROLE_USER).
                boolean hasUserRole = target.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_USER);
                if (!hasUserRole) {
                    try {
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new IllegalStateException("ROLE_USER ist nicht konfiguriert"));
                        target.getRoles().add(userRole);
                        userRepository.save(target);
                        log.info("ROLE_USER automatisch zugewiesen an User: {} (nach Entzug von ADMIN)",
                                targetUsername);
                        try {
                            roleAuditService.record("GRANT", targetUsername, performedBy,
                                    "Assigned ROLE_USER after ADMIN revoke");
                        } catch (Exception e) {
                            log.warn("Audit-Eintrag konnte nicht geschrieben werden: {}", e.getMessage());
                        }
                    } catch (Exception e) {
                        log.warn("Konnte ROLE_USER nicht automatisch zuweisen: {}", e.getMessage());
                    }
                }
            } else {
                log.info("keepAsNoRole=true: keine automatische Zuweisung von ROLE_USER für {}", targetUsername);
            }
        }
    }

    public List<User> listAdmins() {
        return userRepository.findAllByRoles_Name(ERole.ROLE_ADMIN);
    }

    public List<String> listUsernamesByRole(ERole role) {
        return userRepository.findAllByRoles_Name(role).stream().map(User::getUsername).collect(Collectors.toList());
    }

    public boolean isAdmin(String username) {
        try {
            User user = findByUsername(username);
            return user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN);
        } catch (Exception e) {
            return false;
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

    public User updateUserProfile(String username, org.example.javamusicapp.controller.userController.dto.UserUpdateRequest request) {
        User user = findByUsername(username);
        user.setAusbildungsjahr(request.getAusbildungsjahr());
        user.setTelefonnummer(request.getTelefonnummer());
        user.setTeam(request.getTeam());
        return userRepository.save(user);
    }


    /*
     * Lädt ein Profilbild für den angegebenen User hoch, skaliert es bei Bedarf und
     * speichert es im Dateisystem.
     * Unterstützt JPEG-Formate. Löscht das alte Profilbild
     */
    public User uploadProfileImage(String username, MultipartFile file) throws IOException {
        User user = findByUsername(username);

        // Validierung
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Datei ist leer");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Nur Bilddateien sind erlaubt");
        }

        // Lese Rohdaten (damit wir ggf. das Original unverändert speichern können)
        byte[] inputBytes = file.getBytes();
        // Lese Bild aus den Bytes
        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(inputBytes));
        if (image == null) {
            throw new IllegalArgumentException("Die Datei ist kein lesbares Bild");
        }

        // Resize: falls das Bild größer als erlaubt, skaliere proportional
        int origW = image.getWidth();
        int origH = image.getHeight();
        double scale = Math.min(1d, Math.min((double) maxWidth / origW, (double) maxHeight / origH));
        if (scale < 1d) {
            int targetW = (int) Math.round(origW * scale);
            int targetH = (int) Math.round(origH * scale);
            BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = resized.createGraphics();
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // If source has alpha, paint white background first
            if (image.getColorModel().hasAlpha()) {
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, targetW, targetH);
            }
            g2.drawImage(image, 0, 0, targetW, targetH, null);
            g2.dispose();
            image = resized;
        } else {
            // Wenn das Bild Alpha hat, konvertiere zu RGB mit weißem Hintergrund
            if (image.getColorModel().hasAlpha() && image.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = converted.createGraphics();
                g.setComposite(AlphaComposite.SrcOver);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                g.drawImage(image, 0, 0, null);
                g.dispose();
                image = converted;
            }
        }

        // Bestimme Original-Extension (ohne Punkt)
        String originalFilename = file.getOriginalFilename();
        String originalExt = null;
        if (originalFilename != null && originalFilename.contains(".")) {
            originalExt = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        // Stelle sicher, dass ImageIO-Plugins geladen sind (z.B. TwelveMonkeys)
        try {
            javax.imageio.ImageIO.scanForPlugins();
        } catch (Throwable t) {
            log.debug("Fehler beim Scannen von ImageIO-Plugins: {}", t.getMessage());
        }

        // Versuche WebP (unter verschiedenen Format-/MIME-Namen), sonst fallback auf
        // JPEG
        String[] webpNames = new String[] { "webp", "WEBP", "WebP" };
        Iterator<ImageWriter> writers = null;
        for (String name : webpNames) {
            writers = ImageIO.getImageWritersByFormatName(name);
            if (writers.hasNext())
                break;
            writers = ImageIO.getImageWritersByMIMEType("image/" + name.toLowerCase());
            if (writers.hasNext())
                break;
        }

        String newFilename;
        Path targetPath;
        if (writers != null && writers.hasNext()) {
            newFilename = user.getId() + "_" + UUID.randomUUID() + ".webp";
            targetPath = Paths.get(UPLOAD_DIR + newFilename);
            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(targetPath.toFile())) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(imageQuality);
                }
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
            log.info("Profilbild gespeichert als WebP: {}", newFilename);
        } else {
            // Log available writer formats for debugging
            try {
                String[] available = ImageIO.getWriterFormatNames();
                log.info("Keine WebP-Writer gefunden. Verfügbare ImageIO-Writer: {}", String.join(",", available));
            } catch (Throwable t) {
                log.debug("Fehler beim Abfragen verfügbarer Writer: {}", t.getMessage());
            }
            // Fallback: schreibe als JPEG
            // If original was WebP and we don't have a WebP writer, save original bytes as
            // .webp
            if ("webp".equals(originalExt)) {
                newFilename = user.getId() + "_" + UUID.randomUUID() + ".webp";
                targetPath = Paths.get(UPLOAD_DIR + newFilename);
                try {
                    Files.write(targetPath, inputBytes);
                    log.info(
                            "Original WebP-Datei gespeichert (keine WebP-Writer verfügbar, Originalbytes verwendet): {}",
                            newFilename);
                } catch (IOException e) {
                    log.error("Fehler beim Speichern der Original-WebP-Datei: {}", e.getMessage());
                    throw e;
                }
            } else {
                newFilename = user.getId() + "_" + UUID.randomUUID() + ".jpg";
                targetPath = Paths.get(UPLOAD_DIR + newFilename);
                Iterator<ImageWriter> jpgWriters = ImageIO.getImageWritersByFormatName("jpg");
                if (!jpgWriters.hasNext()) {
                    ImageIO.write(image, "jpg", targetPath.toFile());
                } else {
                    ImageWriter writer = jpgWriters.next();
                    try (ImageOutputStream ios = ImageIO.createImageOutputStream(targetPath.toFile())) {
                        writer.setOutput(ios);
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        if (param.canWriteCompressed()) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(imageQuality);
                        }
                        writer.write(null, new IIOImage(image, null, null), param);
                    } finally {
                        writer.dispose();
                    }
                }
                log.info("Profilbild gespeichert als JPEG (WebP-Writer nicht verfügbar): {}", newFilename);
            }
        }

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
