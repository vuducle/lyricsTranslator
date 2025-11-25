package org.example.javamusicapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/nachweise")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NachweisController {

    private final NachweisService nachweisService;
    private final PdfExportService pdfExportService;
    private final NachweisRepository nachweisRepository;

    private final Path rootLocation = Paths.get("generated_pdfs");

    @PostMapping
    @Operation(summary = "Create a new Nachweis and generate a PDF.",
            description = "Creates a new Nachweis, saves it, generates a PDF and stores it on the server.")
    @ApiResponse(responseCode = "201", description = "Nachweis created successfully.")
    @ApiResponse(responseCode = "500", description = "Internal server error during PDF generation or saving.")
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

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Get a Nachweis PDF by its ID.",
            description = "Retrieves the PDF of a specific Nachweis. Only accessible by the owner or an admin.")
    @ApiResponse(responseCode = "200", description = "PDF found and returned.")
    @ApiResponse(responseCode = "403", description = "Forbidden - you are not the owner of this Nachweis.")
    @ApiResponse(responseCode = "404", description = "Nachweis or PDF not found.")
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
