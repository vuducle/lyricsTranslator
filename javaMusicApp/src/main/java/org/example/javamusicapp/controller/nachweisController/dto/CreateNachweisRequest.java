package org.example.javamusicapp.controller.nachweisController.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateNachweisRequest {
    @Schema(description = "Startdatum der Nachweiswoche", example = "2025-11-24")
    private LocalDate datumStart;
    @Schema(description = "Enddatum der Nachweiswoche", example = "2025-11-28")
    private LocalDate datumEnde;
    @Schema(description = "Die Nummer des Nachweises", example = "42")
    private int nummer;
    @Schema(description = "Liste der Aktivitäten für die Woche. Wenn nicht angegeben, wird eine Standardliste erstellt.")
    private List<ActivityDTO> activities;
    @Schema(description = "ID des Ausbilders. Wenn nicht angegeben, wird ein Standard-Ausbilder zugewiesen.", example = "e27590d3-657d-4feb-bd4e-1ffca3d7a884")
    private UUID ausbilderId;
    @Schema(description = "E-Mail-Adresse des Ausbilders, an die der Nachweis gesendet werden soll", example = "ausbilder@example.com")
    private String ausbilderEmail;
    @Schema(description = "Das Ausbildungsjahr (z.B. '1. Ausbildungsjahr', '2. Ausbildungsjahr')", example = "2. Ausbildungsjahr")
    private String ausbildungsjahr;
}
