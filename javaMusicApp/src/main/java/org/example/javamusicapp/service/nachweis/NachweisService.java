package org.example.javamusicapp.service.nachweis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.controller.nachweisController.dto.CreateNachweisRequest;
import org.example.javamusicapp.exception.ResourceNotFoundException;
import org.example.javamusicapp.exception.UnauthorizedActionException;
import org.example.javamusicapp.model.Activity;
import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.model.enums.EStatus;
import org.example.javamusicapp.model.enums.Weekday;
import org.example.javamusicapp.repository.NachweisRepository;
import org.example.javamusicapp.repository.UserRepository;
import org.example.javamusicapp.service.auth.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NachweisService {

    private final NachweisRepository nachweisRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final EmailService emailService; // Inject EmailService
    private final PdfExportService pdfExportService; // Inject PdfExportService
    private final NachweisAuditService nachweisAuditService; // Inject NachweisAuditService

    private final Path rootLocation = Paths.get("generated_pdfs");

    @Transactional
    public Nachweis erstelleNachweis(CreateNachweisRequest request, String username) {
        User user = userService.findByUsername(username);

        User ausbilder = userRepository.findById(request.getAusbilderId())
                .orElseThrow(() -> new ResourceNotFoundException("Ausbilder nicht gefunden."));

        Nachweis nachweis = new Nachweis();
        nachweis.setName(user.getName());
        nachweis.setDatumStart(request.getDatumStart());
        nachweis.setDatumEnde(request.getDatumEnde());
        nachweis.setNummer(request.getNummer());
        nachweis.setAusbildungsjahr(request.getAusbildungsjahr());
        nachweis.setAzubi(user);
        nachweis.setAusbilder(ausbilder);
        nachweis.setStatus(EStatus.IN_BEARBEITUNG);

        if (request.getActivities() == null || request.getActivities().isEmpty()) {
            // Create default activities
            nachweis.addActivity(createActivity(Weekday.MONDAY, 1, "Schule", new BigDecimal("8.0"), "Theorie"));
            nachweis.addActivity(createActivity(Weekday.TUESDAY, 1, "Teambesprechung mit Triesnha Ameilya",
                    new BigDecimal("1.0"), "Meeting"));
            nachweis.addActivity(
                    createActivity(Weekday.TUESDAY, 2, "Coding mit Vergil", new BigDecimal("7.0"), "Entwicklung"));
            nachweis.addActivity(createActivity(Weekday.WEDNESDAY, 1, "Layoutdesign mit Armin Wache",
                    new BigDecimal("4.0"), "Design"));
            nachweis.addActivity(createActivity(Weekday.WEDNESDAY, 2, "Vibe coding mit Vu Quy Le",
                    new BigDecimal("4.0"), "Entwicklung"));
            nachweis.addActivity(
                    createActivity(Weekday.THURSDAY, 1, "Coding mit Vergil", new BigDecimal("8.0"), "Entwicklung"));
            nachweis.addActivity(
                    createActivity(Weekday.FRIDAY, 1, "Coding mit Vergil", new BigDecimal("7.0"), "Entwicklung"));
            nachweis.addActivity(createActivity(Weekday.FRIDAY, 2, "Code Review", new BigDecimal("1.0"), "QA"));
        } else {
            request.getActivities().forEach(activityDTO -> {
                Activity activity = new Activity();
                activity.setDay(activityDTO.getDay());
                activity.setSlot(activityDTO.getSlot());
                activity.setDescription(activityDTO.getDescription());
                activity.setHours(activityDTO.getHours());
                activity.setSection(activityDTO.getSection());
                nachweis.addActivity(activity);
            });
        }

        Nachweis savedNachweis = nachweisRepository.save(nachweis); // Save first to get ID
        nachweisAuditService.loggeNachweisAktion(savedNachweis.getId(), "ERSTELLT", username, null, savedNachweis);

        try {
            byte[] pdfBytes = pdfExportService.generateAusbildungsnachweisPdf(savedNachweis);
            UUID userId = savedNachweis.getAzubi().getId();
            String userVollerName = savedNachweis.getAzubi().getName()
                    .toLowerCase()
                    .replaceAll(" ", "_");
            UUID nachweisId = savedNachweis.getId();

            Path userDirectory = rootLocation.resolve(userVollerName + "_" + userId.toString());
            Files.createDirectories(userDirectory);
            Path destinationFile = userDirectory.resolve(nachweisId.toString() + ".pdf");
            Files.write(destinationFile, pdfBytes);

            // Send email if ausbilder has an email
            String ausbilderEmail = ausbilder.getEmail();
            log.debug("Evaluating email sending for Nachweis {}. Ausbilder Email from user object: {}",
                    savedNachweis.getId(), ausbilderEmail);
            if (ausbilderEmail != null && !ausbilderEmail.isEmpty()) {
                String ausbilderName = ausbilder.getName();
                String azubiName = user.getName();
                String nachweisNummer = String.valueOf(savedNachweis.getNummer());
                String datumStartFormatted = "N/A";
                String datumEndeFormatted = "N/A";
                String ausbildungsjahr = "N/A";

                if (savedNachweis.getDatumStart() != null) {
                    datumStartFormatted = savedNachweis.getDatumStart()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                } else {
                    log.warn("DatumStart is null for Nachweis ID: {}", savedNachweis.getId());
                }

                if (savedNachweis.getDatumEnde() != null) {
                    datumEndeFormatted = savedNachweis.getDatumEnde()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                } else {
                    log.warn("DatumEnde is null for Nachweis ID: {}", savedNachweis.getId());
                }

                if (savedNachweis.getAusbildungsjahr() != null && !savedNachweis.getAusbildungsjahr().isEmpty()) {
                    ausbildungsjahr = savedNachweis.getAusbildungsjahr();
                } else {
                    log.warn("Ausbildungsjahr is null or empty for Nachweis ID: {}", savedNachweis.getId());
                }

                String subject = "Neuer Ausbildungsnachweis von " + azubiName;
                String body = "<html>"
                        + "<head>"
                        + "<meta charset='utf-8'/>"
                        + "<style>"
                        + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial; color: #0f172a; line-height:1.5;}"
                        + ".container { max-width:640px; margin:0 auto; padding:20px; border-radius:12px; background:#fff; box-shadow:0 6px 20px rgba(16,24,40,0.06);}"
                        + ".header { background: linear-gradient(90deg,#1DB954 0%,#16a34a 100%); color:#fff; padding:18px; text-align:center; border-radius:10px 10px 0 0;}"
                        + ".content { padding:20px; color:#0f172a;}"
                        + ".muted { color:#64748b; font-size:0.9em; }"
                        + ".btn { display:inline-block; background:#1DB954; color:#fff; padding:10px 16px; border-radius:999px; text-decoration:none; font-weight:600; }"
                        + "ul { margin:8px 0 12px 18px; }"
                        + "</style>"
                        + "</head>"
                        + "<body>"
                        + "<div class='container'>"
                        + "<div class='header'>"
                        + "<h2 style='margin:0;font-size:18px;'>Neuer Nachweis eingereicht</h2>"
                        + "</div>"
                        + "<div class='content'>"
                        + "<p>Hey " + ausbilderName + " 👋</p>"
                        + "<p>Dein Azubi <strong>" + azubiName
                        + "</strong> hat einen neuen Ausbildungsnachweis eingereicht.</p>"
                        + "<p><strong>Kurzinfo</strong></p>"
                        + "<ul>"
                        + "<li><strong>Nummer:</strong> " + nachweisNummer + "</li>"
                        + "<li><strong>Zeitraum:</strong> " + datumStartFormatted + " - " + datumEndeFormatted + "</li>"
                        + "<li><strong>Ausbildungsjahr:</strong> " + ausbildungsjahr + "</li>"
                        + "</ul>"
                        + "<p class='muted'>Den kompletten Nachweis findest du als PDF im Anhang.</p>"
                        + "<p>Danke & beste Grüße,<br/>" + azubiName + "</p>"
                        + "</div>"
                        + "<div style='padding:12px;text-align:center;color:#94a3b8;font-size:12px;'>"
                        + "Automatisch generierte Nachricht — bitte nicht direkt antworten."
                        + "<p style='text-align:center;margin:14px 0;'><a class='btn' href='https://github.com/vuducle/javaSpringBootApp/' target='_blank' rel='noopener'>Quellcode ansehen</a></p>"
                        + "<p>Mit viel Liebe mit Java gecodet ❤\uFE0F\uD83C\uDDEE\uD83C\uDDE9\uD83C\uDDFB\uD83C\uDDF3☕\uFE0F</p>"
                        + "</div>"
                        + "</div>"
                        + "</body>"
                        + "</html>";
                emailService.sendEmailWithAttachment(
                        ausbilderEmail,
                        subject,
                        body,
                        pdfBytes,
                        "Ausbildungsnachweis_" + nachweisId + ".pdf",
                        "application/pdf");
            }

            return savedNachweis;
        } catch (IOException e) {
            log.error("Fehler bei der PDF-Generierung oder Speicherung für Nachweis {}: {}", savedNachweis.getId(),
                    e.getMessage());
            throw new RuntimeException("Fehler bei der PDF-Generierung oder Speicherung", e);
        }
    }

    public Page<Nachweis> kriegeNachweiseVonAzubiBenutzername(String username, int page, int size) {
        User azubi = userService.findByUsername(username);
        Pageable pageable = PageRequest.of(page, size);
        return nachweisRepository.findAllByAzubiId(azubi.getId(), pageable);
    }

    public Page<Nachweis> findAllNachweise(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return nachweisRepository.findAll(pageable);
    }

    public Page<Nachweis> findNachweiseByUserId(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return nachweisRepository.findAllByAzubiId(userId, pageable);
    }

    public Page<Nachweis> kriegeNachweiseVonAzubiBenutzernameMitFilterUndPagination(String username, EStatus status,
            int page, int size) {
        User azubi = userService.findByUsername(username);
        Pageable pageable = PageRequest.of(page, size);

        if (status != null) {
            return nachweisRepository.findAllByAzubiIdAndStatus(azubi.getId(), status, pageable);
        } else {
            return nachweisRepository.findAllByAzubiId(azubi.getId(), pageable);
        }
    }

    public Page<Nachweis> kriegeAlleNachweiseMitFilterUndPagination(EStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            return nachweisRepository.findAllByStatus(status, pageable);
        } else {
            return nachweisRepository.findAll(pageable);
        }
    }

    public Page<Nachweis> findNachweiseByUserIdMitFilterUndPagination(UUID userId, EStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            return nachweisRepository.findAllByAzubiIdAndStatus(userId, status, pageable);
        } else {
            return nachweisRepository.findAllByAzubiId(userId, pageable);
        }
    }

    @Transactional
    public void loescheNachweis(UUID id, String username) {
        Nachweis nachweis = nachweisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nachweis mit der ID nicht gefunden: " + id));

        nachweisAuditService.loggeNachweisAktion(nachweis.getId(), "GELOESCHT", username, nachweis, null);

        String userVollerName = nachweis.getAzubi().getName().toLowerCase().replaceAll(" ", "_");
        Path userDirectory = rootLocation.resolve(userVollerName + "_" + nachweis.getAzubi().getId().toString());
        Path fileToDelete = userDirectory.resolve(nachweis.getId().toString() + ".pdf");
        deletePdfFile(fileToDelete, nachweis.getId());

        nachweisRepository.deleteById(id);
    }

    private Activity createActivity(Weekday day, Integer slot, String description, BigDecimal hours, String section) {
        Activity activity = new Activity();
        activity.setDay(day);
        activity.setSlot(slot);
        activity.setDescription(description);
        activity.setHours(hours);
        activity.setSection(section);
        return activity;
    }

    private void deletePdfFile(Path filePath, UUID nachweisId) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("PDF-Datei für Nachweis {} erfolgreich gelöscht: {}", nachweisId, filePath);
            } else {
                log.warn("PDF-Datei für Nachweis {} nicht gefunden, konnte nicht gelöscht werden: {}", nachweisId,
                        filePath);
            }
        } catch (IOException e) {
            log.error("Fehler beim Löschen der PDF-Datei für Nachweis {}: {} - {}", nachweisId, filePath,
                    e.getMessage());
            // Optional: throw a custom exception or rethrow as a more specific runtime
            // exception
            // throw new PdfDeletionException("Failed to delete PDF for Nachweis " +
            // nachweisId, e);
        }
    }

    @Transactional
    public void loescheAlleNachweise() {
        List<Nachweis> allNachweise = nachweisRepository.findAll();
        for (Nachweis nachweis : allNachweise) {
            String userVollerName = nachweis.getAzubi().getName().toLowerCase().replaceAll(" ", "_");
            Path userDirectory = rootLocation.resolve(userVollerName + "_" + nachweis.getAzubi().getId().toString());
            Path fileToDelete = userDirectory.resolve(nachweis.getId().toString() + ".pdf");
            deletePdfFile(fileToDelete, nachweis.getId());
        }
        try {
            if (Files.exists(rootLocation)) {
                Files.walk(rootLocation)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(file -> {
                            try {
                                Files.delete(file.toPath());
                                log.debug("Gelöschte Datei/Verzeichnis: {}", file.toPath());
                            } catch (IOException e) {
                                log.error("Fehler beim Löschen von Datei/Verzeichnis {}: {}", file.toPath(),
                                        e.getMessage());
                            }
                        });
                log.info("Alle PDF-Dateien und Verzeichnisse unter {} erfolgreich gelöscht.", rootLocation);
            } else {
                log.info("Verzeichnis {} existiert nicht, keine PDF-Dateien zu löschen.", rootLocation);
            }
        } catch (IOException e) {
            log.error("Fehler beim Löschen des Verzeichnisses {}: {}", rootLocation, e.getMessage());
        }
        nachweisRepository.deleteAll();
    }

    @Transactional
    public void loescheAlleNachweiseVonAzubi(String username) {
        User azubi = userService.findByUsername(username);
        List<Nachweis> nachweise = nachweisRepository.findAllByAzubiId(azubi.getId());

        for (Nachweis nachweis : nachweise) {
            String userVollerName = nachweis.getAzubi().getName().toLowerCase().replaceAll(" ", "_");
            Path userDirectory = rootLocation.resolve(userVollerName + "_" + nachweis.getAzubi().getId().toString());
            Path fileToDelete = userDirectory.resolve(nachweis.getId().toString() + ".pdf");
            deletePdfFile(fileToDelete, nachweis.getId());
        }
        nachweisRepository.deleteAll(nachweise);
    }

    @Transactional
    public Nachweis updateNachweisStatus(UUID nachweisId, EStatus neuerStatus, String comment, String username) {
        Nachweis alterNachweis = nachweisRepository.findById(nachweisId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Nachweis mit der ID " + nachweisId + " nicht gefunden."));
        // Eine Kopie des alten Nachweises erstellen, um den Zustand vor der Änderung zu speichern
        Nachweis alterNachweisKopie = new Nachweis(alterNachweis); // Annahme: Es gibt einen Kopierkonstruktor

        alterNachweis.setStatus(neuerStatus);
        alterNachweis.setComment(comment);
        Nachweis updatedNachweis = nachweisRepository.save(alterNachweis);

        nachweisAuditService.loggeNachweisAktion(updatedNachweis.getId(), "STATUS_AKTUALISIERT", username, alterNachweisKopie, updatedNachweis);

        // Send email to Azubi about status update
        User azubi = updatedNachweis.getAzubi();
        if (azubi != null && azubi.getEmail() != null && !azubi.getEmail().isEmpty()) {
            if (neuerStatus == EStatus.ANGENOMMEN) {
                // Send an acceptance email and attach the generated PDF if available
                String subject = "Dein Ausbildungsnachweis Nr. " + updatedNachweis.getNummer() + " wurde angenommen";
                String body = "<html>"
                        + "<head>"
                        + "<meta charset='utf-8'/>"
                        + "<style>"
                        + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial; line-height: 1.5; color: #0f172a; }"
                        + ".container { max-width: 640px; margin: 0 auto; padding: 20px; border-radius: 12px; background-color: #ffffff; box-shadow: 0 6px 20px rgba(16,24,40,0.06); }"
                        + ".header { background: linear-gradient(90deg, #1DB954 0%, #16a34a 100%); color: #ffffff; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }"
                        + ".content { padding: 22px; color: #0f172a; }"
                        + ".footer { text-align: center; font-size: 0.85em; color: #64748b; margin-top: 18px; }"
                        + ".cta { display: inline-block; padding: 10px 18px; background: #1DB954; color: #fff; border-radius: 999px; text-decoration: none; font-weight: 600; }"
                        + ".btn { display:inline-block; background:#1DB954; color:#fff; padding:10px 16px; border-radius:999px; text-decoration:none; font-weight:600; }"
                        + "p { margin: 0 0 12px 0; }"
                        + "</style>"
                        + "</head>"
                        + "<body>"
                        + "<div class='container'>"
                        + "<div class='header'>"
                        + "<h2 style='margin:0;font-size:20px;'>Nice — dein Nachweis ist angenommen 🎉</h2>"
                        + "</div>"
                        + "<div class='content'>"
                        + "<p>Hallo " + azubi.getName() + ",</p>"
                        + "<p>dein Ausbildungsnachweis Nr. <strong>" + updatedNachweis.getNummer()
                        + "</strong> wurde von deinem Ausbilder angenommen.</p>"
                        + "<p>Herzlichen Glückwunsch! Du findest den Nachweis im Anhang dieser E-Mail.</p>"
                        + "<p>Mit freundlichen Grüßen,</p>"
                        + "<p>Dein Ausbilder/in " + updatedNachweis.getAusbilder().getName() + "</p>"
                        + "</div>"
                        + "<div class='footer'>"
                        + "<p>Dies ist eine automatisch generierte E-Mail. Bitte antworte nicht direkt auf diese Nachricht.</p>"
                        + "<p style='text-align:center;margin:14px 0;'><a class='btn' href='https://github.com/vuducle/javaSpringBootApp/' target='_blank' rel='noopener'>Quellcode ansehen</a></p>"
                        + "<p>Mit viel Liebe mit Java gecodet ❤\uFE0F\uD83C\uDDEE\uD83C\uDDE9\uD83C\uDDFB\uD83C\uDDF3☕\uFE0F</p>"
                        + "</div>"
                        + "</div>"
                        + "</body>"
                        + "</html>";

                // Try to attach the PDF if it exists
                try {
                    String userVollerName = azubi.getName().toLowerCase().replaceAll(" ", "_");
                    Path userDirectory = rootLocation.resolve(userVollerName + "_" + azubi.getId().toString());
                    Path file = userDirectory.resolve(updatedNachweis.getId().toString() + ".pdf");
                    if (Files.exists(file) && Files.isReadable(file)) {
                        byte[] pdfBytes = Files.readAllBytes(file);
                        emailService.sendEmailWithAttachment(
                                azubi.getEmail(),
                                subject,
                                body,
                                pdfBytes,
                                "Ausbildungsnachweis_" + updatedNachweis.getId() + ".pdf",
                                "application/pdf");
                    } else {
                        log.warn("PDF for Nachweis {} not found to attach to acceptance email: {}",
                                updatedNachweis.getId(), file);
                        emailService.sendEmail(azubi.getEmail(), subject, body);
                    }
                } catch (IOException e) {
                    log.error("Fehler beim Lesen der PDF für Nachweis {}: {}", updatedNachweis.getId(), e.getMessage());
                    // Fallback to sending email without attachment
                    emailService.sendEmail(azubi.getEmail(), subject, body);
                }
            } else {
                String subject = "Update zu deinem Ausbildungsnachweis Nr. " + updatedNachweis.getNummer();
                String body = "<html>"
                        + "<head>"
                        + "<meta charset='utf-8'/>"
                        + "<style>"
                        + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial; line-height: 1.5; color: #0f172a; }"
                        + ".container { max-width: 640px; margin: 0 auto; padding: 20px; border-radius: 12px; background-color: #ffffff; box-shadow: 0 6px 20px rgba(16,24,40,0.06); }"
                        + ".header { background: linear-gradient(90deg, #1DB954 0%, #16a34a 100%); color: #ffffff; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }"
                        + ".content { padding: 22px; color: #0f172a; }"
                        + ".footer { text-align: center; font-size: 0.85em; color: #64748b; margin-top: 18px; }"
                        + "p { margin: 0 0 12px 0; }"
                        + "strong { color: #065f46; }"
                        + "</style>"
                        + "</head>"
                        + "<body>"
                        + "<div class='container'>"
                        + "<div class='header'>"
                        + "<h2 style='margin:0;font-size:20px;'>Update zu deinem Nachweis</h2>"
                        + "</div>"
                        + "<div class='content'>"
                        + "<p>Hallo " + azubi.getName() + ",</p>"
                        + "<p>der Status deines Ausbildungsnachweises Nr. <strong>" + updatedNachweis.getNummer()
                        + "</strong> wurde aktualisiert.</p>"
                        + "<p>Neuer Status: <strong>" + neuerStatus.toString() + "</strong></p>"
                        + (comment != null && !comment.isEmpty()
                                ? "<p>Kommentar deines Ausbilders: <em>" + comment + "</em></p>"
                                : "")
                        + "<p>Mit freundlichen Grüßen,</p>"
                        + "<p>Dein Ausbilder/in " + updatedNachweis.getAusbilder().getName() + "</p>"
                        + "</div>"
                        + "<div class='footer'>"
                        + "<p>Dies ist eine automatisch generierte E-Mail. Bitte antworten Sie nicht direkt auf diese Nachricht.</p>"
                        + "<p style='text-align:center;margin:14px 0;'><a class='btn' href='https://github.com/vuducle/javaSpringBootApp/' target='_blank' rel='noopener'>Quellcode ansehen</a></p>"
                        + "<p>Mit viel Liebe mit Java gecodet ❤\uFE0F\uD83C\uDDEE\uD83C\uDDE9\uD83C\uDDFB\uD83C\uDDF3☕\uFE0F</p>"
                        + "</div>"
                        + "</div>"
                        + "</body>"
                        + "</html>";
                emailService.sendEmail(azubi.getEmail(), subject, body);
            }
        }
        return updatedNachweis;
    }

    @Transactional
    public Nachweis aktualisiereNachweisDurchAzubi(UUID nachweisId, CreateNachweisRequest request, String username) {
        Nachweis alterNachweis = nachweisRepository.findById(nachweisId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Nachweis mit der ID " + nachweisId + " nicht gefunden."));
        Nachweis alterNachweisKopie = new Nachweis(alterNachweis); // Kopie für Audit-Log

        User azubi = userService.findByUsername(username);
        if (!alterNachweis.getAzubi().getId().equals(azubi.getId())) {
            throw new UnauthorizedActionException("Sie sind nicht berechtigt, diesen Nachweis zu aktualisieren.");
        }

        User ausbilder = userRepository.findById(request.getAusbilderId())
                .orElseThrow(() -> new ResourceNotFoundException("Ausbilder nicht gefunden."));

        alterNachweis.setDatumStart(request.getDatumStart());
        alterNachweis.setDatumEnde(request.getDatumEnde());
        alterNachweis.setNummer(request.getNummer());
        alterNachweis.setAusbildungsjahr(request.getAusbildungsjahr());
        alterNachweis.setAusbilder(ausbilder);
        alterNachweis.setStatus(EStatus.IN_BEARBEITUNG); // Reset status to IN_BEARBEITUNG

        // Clear existing activities and add new ones
        alterNachweis.getActivities().clear();
        if (request.getActivities() != null && !request.getActivities().isEmpty()) {
            request.getActivities().forEach(activityDTO -> {
                Activity activity = new Activity();
                activity.setDay(activityDTO.getDay());
                activity.setSlot(activityDTO.getSlot());
                activity.setDescription(activityDTO.getDescription());
                activity.setHours(activityDTO.getHours());
                activity.setSection(activityDTO.getSection());
                alterNachweis.addActivity(activity);
            });
        } else {
            // Re-add default activities if none provided
            alterNachweis.addActivity(createActivity(Weekday.MONDAY, 1, "Schule", new BigDecimal("8.0"), "Theorie"));
            alterNachweis.addActivity(createActivity(Weekday.TUESDAY, 1, "Teambesprechung mit Triesnha Ameilya",
                    new BigDecimal("1.0"), "Meeting"));
            alterNachweis.addActivity(
                    createActivity(Weekday.TUESDAY, 2, "Coding mit Vergil", new BigDecimal("7.0"), "Entwicklung"));
            alterNachweis.addActivity(createActivity(Weekday.WEDNESDAY, 1, "Layoutdesign mit Armin Wache",
                    new BigDecimal("4.0"), "Design"));
            alterNachweis.addActivity(createActivity(Weekday.WEDNESDAY, 2, "Vibe coding mit Vu Quy Le",
                    new BigDecimal("4.0"), "Entwicklung"));
            alterNachweis.addActivity(
                    createActivity(Weekday.THURSDAY, 1, "Coding mit Vergil", new BigDecimal("8.0"), "Entwicklung"));
            alterNachweis.addActivity(
                    createActivity(Weekday.FRIDAY, 1, "Coding mit Vergil", new BigDecimal("7.0"), "Entwicklung"));
            alterNachweis.addActivity(createActivity(Weekday.FRIDAY, 2, "Code Review", new BigDecimal("1.0"), "QA"));
        }

        Nachweis updatedNachweis = nachweisRepository.save(alterNachweis);
        nachweisAuditService.loggeNachweisAktion(updatedNachweis.getId(), "AKTUALISIERT_AZUBI", username, alterNachweisKopie, updatedNachweis);

        try {
            byte[] pdfBytes = pdfExportService.generateAusbildungsnachweisPdf(updatedNachweis);
            UUID userId = updatedNachweis.getAzubi().getId();
            String userVollerName = updatedNachweis.getAzubi().getName().toLowerCase().replaceAll(" ", "_");
            UUID nachweisIdPdf = updatedNachweis.getId();

            Path userDirectory = rootLocation.resolve(userVollerName + "_" + userId.toString());
            Files.createDirectories(userDirectory);
            Path destinationFile = userDirectory.resolve(nachweisIdPdf.toString() + ".pdf");
            Files.write(destinationFile, pdfBytes);

            // Send email to Ausbilder about the update
            User nachweisAusbilder = updatedNachweis.getAusbilder();
            if (nachweisAusbilder != null && nachweisAusbilder.getEmail() != null
                    && !nachweisAusbilder.getEmail().isEmpty()) {
                String subject = "Nachweis aktualisiert: Nr. " + updatedNachweis.getNummer() + " von "
                        + azubi.getName();
                String body = "<html>"
                        + "<head>"
                        + "<meta charset='utf-8'/>"
                        + "<style>"
                        + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial; line-height: 1.5; color: #0f172a; }"
                        + ".container { max-width: 640px; margin: 0 auto; padding: 20px; border-radius: 12px; background-color: #ffffff; box-shadow: 0 6px 20px rgba(16,24,40,0.06); }"
                        + ".header { background: linear-gradient(90deg, #1DB954 0%, #16a34a 100%); color: #ffffff; padding: 18px; text-align: center; border-radius: 10px 10px 0 0; }"
                        + ".content { padding: 20px; color: #0f172a; }"
                        + ".note { background: #f1fdf6; color: #064e3b; padding: 12px; border-radius: 8px; margin: 12px 0; }"
                        + ".footer { text-align: center; font-size: 0.85em; color: #64748b; margin-top: 16px; }"
                        + "p { margin: 0 0 12px 0; }"
                        + "</style>"
                        + "</head>"
                        + "<body>"
                        + "<div class='container'>"
                        + "<div class='header'>"
                        + "<h2 style='margin:0;font-size:18px;'>Ausbildungsnachweis aktualisiert — bitte prüfen</h2>"
                        + "</div>"
                        + "<div class='content'>"
                        + "<p>Hi " + nachweisAusbilder.getName() + " 👋</p>"
                        + "<p>Der Nachweis <strong>Nr. " + updatedNachweis.getNummer() + "</strong> von <strong>"
                        + azubi.getName() + "</strong> wurde aktualisiert und ist wieder zur Prüfung bereit.</p>"
                        + (EStatus.IN_BEARBEITUNG != null
                                ? "<p class='note'>Status: <strong>" + EStatus.IN_BEARBEITUNG.toString()
                                        + "</strong></p>"
                                : "")
                        + "<p>Kurz checken, kurz freigeben — danke! 🙏</p>"
                        + "<p>Beste Grüße,<br/>" + azubi.getName() + "</p>"
                        + "</div>"
                        + "<div class='footer'>"
                        + "<p style='text-align:center;margin:14px 0;'><a class='btn' href='https://github.com/vuducle/javaSpringBootApp/tree/main/src/main/java' target='_blank' rel='noopener'>Quellcode ansehen</a></p>"
                        + "<p>Mit viel Liebe mit Java gecodet ❤\uFE0F\uD83C\uDDEE\uD83C\uDDE9\uD83C\uDDFB\uD83C\uDDF3☕\uFE0F</p>"
                        + "<p>Automatisch generierte Nachricht — Antworten werden nicht überwacht.</p>"
                        + "</div>"
                        + "</div>"
                        + "</body>"
                        + "</html>";
                emailService.sendEmail(nachweisAusbilder.getEmail(), subject, body);
            }
        } catch (IOException e) {
            log.error("Fehler bei der PDF-Generierung oder Speicherung für Nachweis {}: {}", updatedNachweis.getId(),
                    e.getMessage());
            throw new RuntimeException("Fehler bei der PDF-Generierung oder Speicherung", e);
        }

        return updatedNachweis;
    }
}