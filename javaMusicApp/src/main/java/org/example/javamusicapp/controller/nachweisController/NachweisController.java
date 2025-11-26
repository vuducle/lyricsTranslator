package org.example.javamusicapp.controller.nachweisController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.javamusicapp.controller.nachweisController.dto.CreateNachweisRequest;
import org.example.javamusicapp.controller.nachweisController.dto.NachweisStatusUpdateRequest;
import org.example.javamusicapp.model.enums.EStatus;
import org.springframework.data.domain.Page;
import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.repository.NachweisRepository;
import org.example.javamusicapp.service.nachweis.NachweisService;
import org.example.javamusicapp.service.nachweis.PdfExportService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nachweise")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Nachweise", description = "API für die Verwaltung von Ausbildungsnachweisen")
public class NachweisController {

    private final NachweisService nachweisService;
    private final PdfExportService pdfExportService;
    private final NachweisRepository nachweisRepository;

    private final Path rootLocation = Paths.get("generated_pdfs");

    @PostMapping
    @Operation(summary = "Erstellt einen neuen Nachweis und generiert ein PDF.",
            description = "Erstellt einen neuen Nachweis, speichert ihn, generiert ein PDF und legt es auf dem Server ab. " +
                    "Wenn die Aktivitätenliste leer ist, wird eine Standardliste erstellt.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nachweis-Objekt, das dem Speicher hinzugefügt werden muss",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateNachweisRequest.class),
                            examples = @ExampleObject(
                                    name = "Standard-Nachweis",
                                    summary = "Beispiel für einen Nachweis mit Standardaktivitäten",
                                    value = "{\n" +
                                            "  \"datumStart\": \"2025-11-24\",\n" +
                                            "  \"datumEnde\": \"2025-11-28\",\n" +
                                            "  \"nummer\": 42,\n" +
                                            "  \"ausbilderId\": \"e27590d3-657d-4feb-bd4e-1ffca3d7a884\",\n" +
                                            "  \"ausbildungsjahr\": \"2. Ausbildungsjahr\",\n" +
                                            "  \"activities\": []\n" +
                                            "}"
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "201", description = "Nachweis erfolgreich erstellt.")
    @ApiResponse(responseCode = "500", description = "Interner Serverfehler bei der PDF-Generierung oder Speicherung.")
    public ResponseEntity<Nachweis> createNachweis(@RequestBody CreateNachweisRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Nachweis nachweis = nachweisService.erstelleNachweis(request, userDetails.getUsername());

        try {
            byte[] pdfBytes = pdfExportService.generateAusbildungsnachweisPdf(nachweis);
            UUID userId = nachweis.getAzubi().getId();
            UUID nachweisId = nachweis.getId();

            Path userDirectory = rootLocation.resolve(userId.toString());
            Files.createDirectories(userDirectory);
            Path destinationFile = userDirectory.resolve(nachweisId.toString() + ".pdf");
            Files.write(destinationFile, pdfBytes);

            return new ResponseEntity<>(nachweis, HttpStatus.CREATED);
        } catch (IOException e) {
            // Log the exception details
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/my-nachweise")
    @Operation(summary = "Ruft alle Nachweise für den aktuell angemeldeten Azubi ab, mit optionaler Filterung und Pagination.",
            description = "Gibt eine Liste aller Nachweise zurück, die dem aktuell authentifizierten Azubi gehören. Kann nach Status gefiltert und paginiert werden.")
    @ApiResponse(responseCode = "200", description = "Liste der Nachweise erfolgreich abgerufen.")
    @ApiResponse(responseCode = "403", description = "Zugriff verweigert, wenn der Benutzer nicht authentifiziert ist.")
    public ResponseEntity<Page<Nachweis>> getMyNachweise(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) EStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Nachweis> nachweise = nachweisService.kriegeNachweiseVonAzubiBenutzernameMitFilterUndPagination(userDetails.getUsername(), status, page, size);
        return ResponseEntity.ok(nachweise);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Holt ein Nachweis-PDF anhand seiner ID.",
            description = "Ruft das PDF eines bestimmten Nachweises ab. Nur für den Besitzer oder einen Admin zugänglich.")
    @ApiResponse(responseCode = "200", description = "PDF gefunden und zurückgegeben.")
    @ApiResponse(responseCode = "403", description = "Verboten - Sie sind nicht der Besitzer dieses Nachweises.")
    @ApiResponse(responseCode = "404", description = "Nachweis oder PDF nicht gefunden.")
    @PreAuthorize("hasRole('ADMIN') or @nachweisSecurityService.isOwner(authentication, #id)")
    public ResponseEntity<Resource> getNachweisPdf(@PathVariable UUID id) {
        Nachweis nachweis = nachweisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nachweis not found")); // Should be a proper exception

        try {
            Path userDirectory = rootLocation.resolve(nachweis.getAzubi().getId().toString());
            Path file = userDirectory.resolve(nachweis.getId().toString() + ".pdf");
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "ausbildungsnachweis.pdf");
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Löscht einen Nachweis anhand seiner ID.",
            description = "Löscht einen bestimmten Nachweis. Nur der Besitzer oder ein Admin kann einen Nachweis löschen.")
    @ApiResponse(responseCode = "204", description = "Nachweis erfolgreich gelöscht.")
    @ApiResponse(responseCode = "403", description = "Verboten - Sie sind nicht berechtigt, diesen Nachweis zu löschen.")
    @ApiResponse(responseCode = "404", description = "Nachweis nicht gefunden.")
    @PreAuthorize("hasRole('ADMIN') or @nachweisSecurityService.isOwner(authentication, #id)")
    public ResponseEntity<Void> deleteNachweis(@PathVariable UUID id) {
        nachweisService.loescheNachweis(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/all")
    @Operation(summary = "Löscht alle Nachweise und zugehörige PDFs.",
            description = "Löscht alle Nachweise aus der Datenbank und alle generierten PDF-Dateien. Nur für Administratoren zugänglich.")
    @ApiResponse(responseCode = "204", description = "Alle Nachweise und PDFs erfolgreich gelöscht.")
    @ApiResponse(responseCode = "403", description = "Verboten - Nur Administratoren können alle Nachweise löschen.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllNachweise() {
        nachweisService.loescheAlleNachweise();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/my-nachweise/all")
    @Operation(summary = "Löscht alle Nachweise und zugehörige PDFs des aktuell angemeldeten Azubis.",
            description = "Löscht alle Nachweise aus der Datenbank und alle generierten PDF-Dateien, die dem aktuell authentifizierten Azubi gehören.")
    @ApiResponse(responseCode = "204", description = "Alle Nachweise und PDFs des Azubis erfolgreich gelöscht.")
    @ApiResponse(responseCode = "403", description = "Verboten - Zugriff verweigert, wenn der Benutzer nicht authentifiziert ist.")
    @PreAuthorize("hasRole('USER')") // Assuming 'USER' role for regular users
    public ResponseEntity<Void> deleteAllMyNachweise(@AuthenticationPrincipal UserDetails userDetails) {
        nachweisService.loescheAlleNachweiseVonAzubi(userDetails.getUsername());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Ruft alle Nachweise für alle Benutzer ab (Admin-Zugriff).",
            description = "Gibt eine Liste aller Nachweise im System zurück. Nur für Administratoren zugänglich.")
    @ApiResponse(responseCode = "200", description = "Liste aller Nachweise erfolgreich abgerufen.")
    @ApiResponse(responseCode = "403", description = "Verboten - Nur Administratoren können alle Nachweise abrufen.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Nachweis>> getAllNachweise() {
        List<Nachweis> nachweise = nachweisService.findAllNachweise();
        return ResponseEntity.ok(nachweise);
    }

    @GetMapping("/admin/user/{userId}")
    @Operation(summary = "Ruft alle Nachweise für einen bestimmten Benutzer ab (Admin-Zugriff).",
            description = "Gibt eine Liste aller Nachweise für den angegebenen Benutzer zurück. Nur für Administratoren zugänglich.")
    @ApiResponse(responseCode = "200", description = "Liste der Nachweise für den Benutzer erfolgreich abgerufen.")
    @ApiResponse(responseCode = "403", description = "Verboten - Nur Administratoren können Nachweise für andere Benutzer abrufen.")
    @ApiResponse(responseCode = "404", description = "Benutzer nicht gefunden oder keine Nachweise vorhanden.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Nachweis>> getNachweiseByUserId(@PathVariable UUID userId) {
        List<Nachweis> nachweise = nachweisService.findNachweiseByUserId(userId);
        if (nachweise.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(nachweise);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Aktualisiert den Status eines Nachweises (Admin-Zugriff).",
            description = "Ermöglicht Administratoren, den Status eines Nachweises auf ANGENOMMEN oder ABGELEHNT zu setzen.")
    @ApiResponse(responseCode = "200", description = "Nachweisstatus erfolgreich aktualisiert.")
    @ApiResponse(responseCode = "400", description = "Ungültiger Status oder Nachweis-ID.")
    @ApiResponse(responseCode = "403", description = "Verboten - Nur Administratoren können den Nachweisstatus aktualisieren.")
    @ApiResponse(responseCode = "404", description = "Nachweis nicht gefunden.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Nachweis> updateNachweisStatus(@PathVariable UUID id, @RequestBody NachweisStatusUpdateRequest request) {
        if (!id.equals(request.getNachweisId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Nachweis updatedNachweis = nachweisService.updateNachweisStatus(request.getNachweisId(), request.getStatus(), request.getComment());
        return ResponseEntity.ok(updatedNachweis);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aktualisiert einen Nachweis durch den Azubi.",
            description = "Ermöglicht dem Azubi, seinen eigenen Nachweis zu aktualisieren. Der Status wird auf IN_BEARBEITUNG zurückgesetzt.")
    @ApiResponse(responseCode = "200", description = "Nachweis erfolgreich aktualisiert.")
    @ApiResponse(responseCode = "400", description = "Ungültige Anfrage oder Nachweis-ID.")
    @ApiResponse(responseCode = "403", description = "Verboten - Sie sind nicht der Besitzer dieses Nachweises.")
    @ApiResponse(responseCode = "404", description = "Nachweis nicht gefunden.")
    @PreAuthorize("@nachweisSecurityService.isOwner(authentication, #id)")
    public ResponseEntity<Nachweis> updateNachweisByAzubi(@PathVariable UUID id, @RequestBody CreateNachweisRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Nachweis updatedNachweis = nachweisService.aktualisiereNachweisDurchAzubi(id, request, userDetails.getUsername());
        return ResponseEntity.ok(updatedNachweis);
    }
}
