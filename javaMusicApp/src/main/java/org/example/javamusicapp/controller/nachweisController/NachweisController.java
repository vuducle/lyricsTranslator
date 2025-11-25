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
import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.repository.NachweisRepository;
import org.example.javamusicapp.service.NachweisService;
import org.example.javamusicapp.service.PdfExportService;
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
                                            "  \"activities\": []\n" +
                                            "}"
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "201", description = "Nachweis erfolgreich erstellt.")
    @ApiResponse(responseCode = "500", description = "Interner Serverfehler bei der PDF-Generierung oder Speicherung.")
    public ResponseEntity<Nachweis> createNachweis(@RequestBody CreateNachweisRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Nachweis nachweis = nachweisService.createNachweis(request, userDetails.getUsername());

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
    @Operation(summary = "Ruft alle Nachweise für den aktuell angemeldeten Azubi ab.",
            description = "Gibt eine Liste aller Nachweise zurück, die dem aktuell authentifizierten Azubi gehören.")
    @ApiResponse(responseCode = "200", description = "Liste der Nachweise erfolgreich abgerufen.")
    @ApiResponse(responseCode = "403", description = "Zugriff verweigert, wenn der Benutzer nicht authentifiziert ist.")
    public ResponseEntity<List<Nachweis>> getMyNachweise(@AuthenticationPrincipal UserDetails userDetails) {
        List<Nachweis> nachweise = nachweisService.getNachweiseByAzubiUsername(userDetails.getUsername());
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
}
