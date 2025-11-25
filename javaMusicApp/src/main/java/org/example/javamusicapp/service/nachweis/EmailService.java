package org.example.javamusicapp.service.nachweis;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
}
