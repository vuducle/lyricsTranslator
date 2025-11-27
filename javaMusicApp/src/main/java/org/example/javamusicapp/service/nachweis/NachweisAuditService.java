package org.example.javamusicapp.service.nachweis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.model.NachweisAuditLog;
import org.example.javamusicapp.repository.NachweisAuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 🕵️‍♀️ **Was geht hier ab?**
 * Dieser Service ist der Historiker für unsere Ausbildungsnachweise. Er protokolliert
 * jede einzelne Aktion, die mit einem Nachweis passiert.
 *
 * Sein einziger, aber ultra wichtiger Job:
 * - **loggeNachweisAktion()**: Immer wenn ein Nachweis erstellt, bearbeitet oder sein
 *   Status geändert wird, ruft der `NachweisService` diese Methode auf.
 *   Sie speichert dann einen `NachweisAuditLog`-Eintrag in der Datenbank.
 *
 * Das Besondere: Sie speichert den alten Zustand und den neuen Zustand des Nachweises
 * als JSON-String. Dadurch kann man später im Audit-Log ganz genau sehen, welche
 * Felder sich geändert haben. Man weiß also immer, wer was wann geändert hat.
 * Macht alles transparent und nachvollziehbar.
 */
@Service
@Slf4j
public class NachweisAuditService {

    private final NachweisAuditLogRepository nachweisAuditLogRepository;
    private final ObjectMapper objectMapper;

    public NachweisAuditService(NachweisAuditLogRepository nachweisAuditLogRepository) {
        this.nachweisAuditLogRepository = nachweisAuditLogRepository;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.disable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // Make stored JSON human-readable
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void loggeNachweisAktion(UUID nachweisId, String aktion, String benutzerName, Nachweis alterNachweis,
            Nachweis neuerNachweis) {
        String alteDatenJson = null;
        String neueDatenJson = null;

        log.debug("Versuche Nachweis Audit-Log für Nachweis-ID: {}, Aktion: {}, Benutzer: {}", nachweisId, aktion,
                benutzerName);

        try {
            if (alterNachweis != null) {
                log.debug("Starte JSON-Serialisierung für alten Nachweis (ID: {})", alterNachweis.getId());
                alteDatenJson = objectMapper.writeValueAsString(alterNachweis);
                log.debug("Alter Nachweis JSON-Länge: {}", alteDatenJson.length());
            }
            if (neuerNachweis != null) {
                log.debug("Starte JSON-Serialisierung für neuen Nachweis (ID: {})", neuerNachweis.getId());
                neueDatenJson = objectMapper.writeValueAsString(neuerNachweis);
                log.debug("Neuer Nachweis JSON-Länge: {}", neueDatenJson.length());
            }
        } catch (JsonProcessingException e) {
            log.error("Fehler bei der JSON-Serialisierung des Nachweises für Audit-Log (Nachweis-ID: {}): {}",
                    nachweisId, e.getMessage());
            // Transaktion wird hier wahrscheinlich schon als rollback-only markiert
            throw new RuntimeException("Fehler bei der JSON-Serialisierung für Audit-Log", e); // Exzeption weiterwerfen
        }

        NachweisAuditLog auditLog = new NachweisAuditLog(
                null, // ID wird von JPA generiert
                nachweisId,
                aktion,
                LocalDateTime.now(),
                benutzerName,
                alteDatenJson,
                neueDatenJson);

        try {
            log.debug("Versuche Nachweis Audit-Log zu speichern (Nachweis-ID: {}, Aktion: {})", nachweisId, aktion);
            nachweisAuditLogRepository.save(auditLog);
            log.debug("Nachweis Audit-Log erfolgreich gespeichert für Nachweis-ID: {}", nachweisId);
        } catch (Exception e) {
            log.error("Fehler beim Speichern des Nachweis Audit-Logs (Nachweis-ID: {}): {}", nachweisId,
                    e.getMessage());
            // Transaktion wird hier wahrscheinlich schon als rollback-only markiert
            throw new RuntimeException("Fehler beim Speichern des Nachweis Audit-Logs", e); // Exzeption weiterwerfen
        }
    }
}
