package org.example.javamusicapp.service.nachweis;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Async
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String attachmentName, String contentType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            log.debug("Setze E-Mail Versender: {}", senderEmail);
            log.debug("Setze E-Mail Empfänger: {}", to);
            helper.setTo(to);
            log.debug("Setze E-Mail Betreff: {}", subject);
            helper.setSubject(subject);
            log.debug("Setze E-Mail Inhalt (Die ersten 100 Charaktere): {}", body.substring(0, Math.min(body.length(), 100)));
            helper.setText(body, true); // Set HTML content
            log.debug("Füge Anhang hinzu: {} mit Content-Typ {}", attachmentName, contentType);
            helper.addAttachment(attachmentName, new ByteArrayResource(attachment), contentType);

            log.debug("Versuche zu versenden: {}", to);
            mailSender.send(message);
            log.info("E-Mail erfolgreich versendet {} mit Anhang {}", to, attachmentName);
        } catch (MessagingException e) {
            log.error("Fehlgeschalgen bei der Versendung {} mit Anhang {}: {}", to, attachmentName, e.getMessage());
            throw new RuntimeException("Gescheitert", e);
        }
    }

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            log.debug("Setze E-Mail Versender: {}", senderEmail);
            log.debug("Setze E-Mail Empfänger: {}", to);
            helper.setTo(to);
            log.debug("Setze E-Mail Betreff: {}", subject);
            helper.setSubject(subject);
            log.debug("Setze E-Mail Inhalt (Die ersten 100 Charaktere): {}", body.substring(0, Math.min(body.length(), 100)));
            helper.setText(body, true); // Set HTML content

            log.debug("Versuche zu versenden: {}", to);
            mailSender.send(message);
            log.info("E-Mail erfolgreich versendet {}", to);
        } catch (MessagingException e) {
            log.error("Fehlgeschalgen bei der Versendung {}: {}", to, e.getMessage());
            throw new RuntimeException("Gescheitert", e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String resetLink) {
        String subject = "Dein Link zum Zurücksetzen des Passworts";
        String body = "<html>"
                + "<head>"
                + "<meta charset='utf-8'/>"
                + "<style>"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial; color: #0f172a; line-height:1.5;}"
                + ".container { max-width:640px; margin:0 auto; padding:20px; border-radius:12px; background:#fff; box-shadow:0 6px 20px rgba(16,24,40,0.06);}"
                + ".header { background: linear-gradient(90deg,#1DB954 0%,#16a34a 100%); color:#fff; padding:18px; text-align:center; border-radius:10px 10px 0 0;}"
                + ".content { padding:20px; color:#0f172a;}"
                + ".muted { color:#64748b; font-size:0.9em; }"
                + ".btn { display:inline-block; background:#1DB954; color:#fff !important; padding:12px 24px; border-radius:999px; text-decoration:none; font-weight:600; margin: 15px 0;}"
                + ".footer { padding:12px;text-align:center;color:#94a3b8;font-size:12px; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h2 style='margin:0;font-size:18px;'>Passwort zurücksetzen</h2>"
                + "</div>"
                + "<div class='content'>"
                + "<p>Hallo " + name + ",</p>"
                + "<p>wir haben eine Anfrage zum Zurücksetzen deines Passworts erhalten. Klicke auf den Button unten, um ein neues Passwort festzulegen.</p>"
                + "<p style='text-align:center;'><a href='" + resetLink + "' class='btn' target='_blank' rel='noopener'>Passwort jetzt zurücksetzen</a></p>"
                + "<p>Dieser Link ist für 1 Stunde gültig. Wenn du diese Anfrage nicht gestellt hast, kannst du diese E-Mail einfach ignorieren.</p>"
                + "<p>Beste Grüße,<br/>Dein JavaMusicApp Team</p>"
                + "</div>"
                + "<div class='footer'>"
                + "Automatisch generierte Nachricht — bitte nicht direkt antworten."
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        sendEmail(to, subject, body);
    }
}
