package org.example.javamusicapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.controller.nachweisController.dto.CreateNachweisRequest;
import org.example.javamusicapp.model.Activity;
import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.model.enums.EStatus;
import org.example.javamusicapp.model.enums.Weekday;
import org.example.javamusicapp.repository.NachweisRepository;
import org.example.javamusicapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final Path rootLocation = Paths.get("generated_pdfs");

    @Transactional
    public Nachweis erstelleNachweis(CreateNachweisRequest request, String username) {
        User user = userService.findByUsername(username);
        
        User ausbilder = userRepository.findById(request.getAusbilderId())
                .orElseThrow(() -> new RuntimeException("Ausbilder nicht gefunden."));

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
            nachweis.addActivity(createActivity(Weekday.TUESDAY, 1, "Teambesprechung mit Triesnha Ameilya", new BigDecimal("1.0"), "Meeting"));
            nachweis.addActivity(createActivity(Weekday.TUESDAY, 2, "Coding mit Vergil", new BigDecimal("7.0"), "Entwicklung"));
            nachweis.addActivity(createActivity(Weekday.WEDNESDAY, 1, "Layoutdesign mit Armin Wache", new BigDecimal("4.0"), "Design"));
            nachweis.addActivity(createActivity(Weekday.WEDNESDAY, 2, "Vibe coding mit Vu Quy Le", new BigDecimal("4.0"), "Entwicklung"));
            nachweis.addActivity(createActivity(Weekday.THURSDAY, 1, "Coding mit Vergil", new BigDecimal("8.0"), "Entwicklung"));
            nachweis.addActivity(createActivity(Weekday.FRIDAY, 1, "Coding mit Vergil", new BigDecimal("7.0"), "Entwicklung"));
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

        try {
            byte[] pdfBytes = pdfExportService.generateAusbildungsnachweisPdf(savedNachweis);
            UUID userId = savedNachweis.getAzubi().getId();
            UUID nachweisId = savedNachweis.getId();

            Path userDirectory = rootLocation.resolve(userId.toString());
            Files.createDirectories(userDirectory);
            Path destinationFile = userDirectory.resolve(nachweisId.toString() + ".pdf");
            Files.write(destinationFile, pdfBytes);

            // Send email if ausbilder has an email
            String ausbilderEmail = ausbilder.getEmail();
            log.debug("Evaluating email sending for Nachweis {}. Ausbilder Email from user object: {}", savedNachweis.getId(), ausbilderEmail);
            if (ausbilderEmail != null && !ausbilderEmail.isEmpty()) {
                String ausbilderName = ausbilder.getName();
                String azubiName = user.getName();
                String nachweisNummer = String.valueOf(savedNachweis.getNummer());
                String datumStartFormatted = "N/A";
                String datumEndeFormatted = "N/A";
                String ausbildungsjahr = "N/A";

                if (savedNachweis.getDatumStart() != null) {
                    datumStartFormatted = savedNachweis.getDatumStart().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                } else {
                    log.warn("DatumStart is null for Nachweis ID: {}", savedNachweis.getId());
                }

                if (savedNachweis.getDatumEnde() != null) {
                    datumEndeFormatted = savedNachweis.getDatumEnde().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
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
                            + "<style>"
                            + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
                            + ".container { max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px; background-color: #f9f9f9; }"
                            + ".header { background-color: #0056b3; color: #ffffff; padding: 10px 20px; text-align: center; border-radius: 5px 5px 0 0; }"
                            + ".content { padding: 20px; }"
                            + ".footer { text-align: center; font-size: 0.8em; color: #777; margin-top: 20px; }"
                            + "p { margin-bottom: 10px; }"
                            + "</style>"
                            + "</head>"
                            + "<body>"
                            + "<div class='container'>"
                            + "<div class='header'>"
                            + "<h2>Neuer Ausbildungsnachweis</h2>"
                            + "</div>"
                            + "<div class='content'>"
                            + "<p>Hallo " + ausbilderName + ",</p>"
                            + "<p>hier ist der neue Ausbildungsnachweis von <strong>" + azubiName + "</strong>.</p>"
                            + "<p><strong>Details zum Nachweis:</strong></p>"
                            + "<ul>"
                            + "<li><strong>Nummer:</strong> " + nachweisNummer + "</li>"
                            + "<li><strong>Zeitraum:</strong> " + datumStartFormatted + " - " + datumEndeFormatted + "</li>"
                            + "<li><strong>Ausbildungsjahr:</strong> " + ausbildungsjahr + "</li>"
                            + "</ul>"
                            + "<p>Den vollständigen Nachweis finden Sie / findest du im Anhang.</p>"
                            + "<p>Mit freundlichen Grüßen,</p>"
                            + "<p><strong>" + azubiName + "</strong></p>"
                            + "</div>"
                            + "<div class='footer'>"
                            + "<p>Dies ist eine automatisch generierte E-Mail. Bitte antworten Sie nicht direkt auf diese Nachricht.</p>"
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
                    "application/pdf"
                );
            }

            return savedNachweis;
        } catch (IOException e) {
            log.error("Fehler bei der PDF-Generierung oder Speicherung für Nachweis {}: {}", savedNachweis.getId(), e.getMessage());
            throw new RuntimeException("Fehler bei der PDF-Generierung oder Speicherung", e);
        }
    }

    public List<Nachweis> kriegeNachweiseVonAzubiBenutzername(String username) {
        User azubi = userService.findByUsername(username);
        return nachweisRepository.findAllByAzubiId(azubi.getId());
    }

    @Transactional
    public void loescheNachweis(UUID id) {
        Nachweis nachweis = nachweisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nachweis mit der ID nicht gefunden: " + id));

        try {
            Path userDirectory = rootLocation.resolve(nachweis.getAzubi().getId().toString());
            Path fileToDelete = userDirectory.resolve(nachweis.getId().toString() + ".pdf");
            Files.deleteIfExists(fileToDelete);
        } catch (IOException e) {
            // Log the exception details, but don't prevent Nachweis deletion if PDF deletion fails
            log.error("Fehler bei der Löschung der Nachweise {}: {}", id, e.getMessage());
        }

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

    @Transactional
    public void loescheAlleNachweise() {
        List<Nachweis> allNachweise = nachweisRepository.findAll();
        for (Nachweis nachweis : allNachweise) {
            try {
                Path userDirectory = rootLocation.resolve(nachweis.getAzubi().getId().toString());
                Path fileToDelete = userDirectory.resolve(nachweis.getId().toString() + ".pdf");
                Files.deleteIfExists(fileToDelete);
            } catch (IOException e) {
                log.error("Fehler beim Löschen der PDF für Nachweis {}: {}", nachweis.getId(), e.getMessage());
            }
        }
        try {
            Files.walk(rootLocation)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
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
            try {
                Path userDirectory = rootLocation.resolve(nachweis.getAzubi().getId().toString());
                Path fileToDelete = userDirectory.resolve(nachweis.getId().toString() + ".pdf");
                Files.deleteIfExists(fileToDelete);
            } catch (IOException e) {
                log.error("Fehler beim Löschen der PDF für Nachweis {}: {}", nachweis.getId(), e.getMessage());
            }
        }
        nachweisRepository.deleteAll(nachweise);
    }
}
