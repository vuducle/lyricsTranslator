package org.example.javamusicapp.controller.nachweisController.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateNachweisRequest {
    @NotNull(message = "Startdatum darf nicht null sein")
    @Schema(description = "Startdatum der Nachweiswoche", example = "2025-11-24")
    private LocalDate datumStart;

    @NotNull(message = "Enddatum darf nicht null sein")
    @Schema(description = "Enddatum der Nachweiswoche", example = "2025-11-28")
    private LocalDate datumEnde;

    @Min(value = 1, message = "Nachweisnummer muss mindestens 1 sein")
    @Schema(description = "Die Nummer des Nachweises", example = "42")
    private int nummer;

    @Schema(description = "Liste der Aktivitäten für die Woche. Wenn nicht angegeben, wird eine Standardliste erstellt.")
    private List<ActivityDTO> activities;

    @NotNull(message = "Ausbilder-ID darf nicht null sein")
    @Schema(description = "ID des Ausbilders. Wenn nicht angegeben, wird ein Standard-Ausbilder zugewiesen.", example = "e27590d3-657d-4feb-bd4e-1ffca3d7a884")
    private UUID ausbilderId;

    // ausbilderEmail ist optional, daher keine @NotNull oder @NotBlank
    @Schema(description = "E-Mail-Adresse des Ausbilders, an die der Nachweis gesendet werden soll", example = "ausbilder@example.com")
    private String ausbilderEmail;

    @NotBlank(message = "Ausbildungsjahr darf nicht leer sein")
    @Size(max = 50, message = "Ausbildungsjahr darf maximal 50 Zeichen lang sein")
    @Schema(description = "Das Ausbildungsjahr (z.B. '1. Ausbildungsjahr', '2. Ausbildungsjahr')", example = "2. Ausbildungsjahr")
    private String ausbildungsjahr;
}
