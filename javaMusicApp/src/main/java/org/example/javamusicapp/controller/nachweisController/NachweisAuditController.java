package org.example.javamusicapp.controller.nachweisController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.model.NachweisAuditLog;
import org.example.javamusicapp.repository.NachweisAuditLogRepository;
import org.example.javamusicapp.service.nachweis.NachweisSecurityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.example.javamusicapp.controller.nachweisController.dto.AuditPageResponse;
import org.example.javamusicapp.controller.nachweisController.dto.NachweisAuditDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 📜 **Was geht hier ab?**
 * Dieser Controller ist der "Was ist passiert?"-Detektiv für die Ausbildungsnachweise.
 * Er liefert die komplette History zu einem Nachweis. Admins, Ausbilder und der Azubi selbst
 * können hier ganz genau nachschauen, was mit einem Nachweis passiert ist.
 *
 * Die Endpunkte zeigen:
 * - / :** Eine Liste aller Änderungen an allen Nachweisen (nur für Admins/Ausbilder).
 * - /{nachweisId}**: Die komplette History für EINEN bestimmten Nachweis. Also wer hat ihn
 *   erstellt, wann wurde er eingereicht, wer hat ihn genehmigt oder abgelehnt und welche
 *   Daten haben sich dabei geändert (vorher/nachher Vergleich).
 *
 * Ultra wichtig für die Nachvollziehbarkeit und wenn's mal Diskussionen gibt.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/nachweis-audit")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Nachweis-Audit (Erstellen, Anpassen, Ablehnen, Annehmen)")
@SecurityRequirement(name = "bearerAuth")
public class NachweisAuditController {

    private final NachweisAuditLogRepository auditRepository;
    private final NachweisSecurityService nachweisSecurityService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Nachweis-Audit anzeigen", description = "Gibt die Audit-Events für einen bestimmten Nachweis zurück (paginiert).")
    @GetMapping("/{nachweisId}")
    @PreAuthorize("hasRole('ADMIN') or @nachweisSecurityService.isAusbilder(authentication) or @nachweisSecurityService.isOwner(authentication, #nachweisId)")
    public ResponseEntity<AuditPageResponse<NachweisAuditDto>> getAuditForNachweis(Authentication authentication,
            @PathVariable("nachweisId") UUID nachweisId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NachweisAuditLog> audits = auditRepository.findAllByNachweisId(nachweisId, pageable);
        List<NachweisAuditDto> items = new ArrayList<>();
        for (NachweisAuditLog a : audits.getContent()) {
            JsonNode alte = null;
            JsonNode neu = null;
            try {
                alte = a.getAlteDaten() != null ? objectMapper.readTree(a.getAlteDaten()) : null;
                neu = a.getNeueDaten() != null ? objectMapper.readTree(a.getNeueDaten()) : null;
            } catch (Exception ignored) {
            }

            NachweisAuditDto dto = NachweisAuditDto.builder()
                    .id(a.getId())
                    .nachweisId(a.getNachweisId())
                    .aktion(a.getAktion())
                    .aktionsZeit(a.getAktionsZeit())
                    .benutzerName(a.getBenutzerName())
                    .alteDaten(alte)
                    .neueDaten(neu)
                    .build();
            items.add(dto);
        }

        AuditPageResponse<NachweisAuditDto> resp = new AuditPageResponse<>(items, audits.getNumber(), audits.getSize(),
                audits.getTotalPages(), audits.getTotalElements());
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Alle Nachweis-Audit-Einträge", description = "Gibt alle Audit-Events paginiert zurück. Nur für Admins/Ausbilder.")
    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN') or @nachweisSecurityService.isAusbilder(authentication)")
    public ResponseEntity<AuditPageResponse<NachweisAuditDto>> getAllAudit(Authentication authentication,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NachweisAuditLog> audits = auditRepository.findAll(pageable);
        List<NachweisAuditDto> items = new ArrayList<>();
        for (NachweisAuditLog a : audits.getContent()) {
            JsonNode alte = null;
            JsonNode neu = null;
            try {
                alte = a.getAlteDaten() != null ? objectMapper.readTree(a.getAlteDaten()) : null;
                neu = a.getNeueDaten() != null ? objectMapper.readTree(a.getNeueDaten()) : null;
            } catch (Exception ignored) {
            }

            NachweisAuditDto dto = NachweisAuditDto.builder()
                    .id(a.getId())
                    .nachweisId(a.getNachweisId())
                    .aktion(a.getAktion())
                    .aktionsZeit(a.getAktionsZeit())
                    .benutzerName(a.getBenutzerName())
                    .alteDaten(alte)
                    .neueDaten(neu)
                    .build();
            items.add(dto);
        }

        AuditPageResponse<NachweisAuditDto> resp = new AuditPageResponse<>(items, audits.getNumber(), audits.getSize(),
                audits.getTotalPages(), audits.getTotalElements());
        return ResponseEntity.ok(resp);
    }
}
