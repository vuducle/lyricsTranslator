package org.example.javamusicapp.controller.nachweisController.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.javamusicapp.model.enums.Weekday;

import java.math.BigDecimal;

@Data
public class ActivityDTO {
    @NotNull(message = "Wochentag darf nicht null sein")
    @Schema(description = "Wochentag für die Aktivität", example = "MONDAY")
    private Weekday day;

    @NotNull(message = "Slot darf nicht null sein")
    @Min(value = 1, message = "Slot muss mindestens 1 sein")
    @Schema(description = "Zeitschlitz für die Aktivität (z.B. 1 für die erste Aufgabe des Tages)", example = "1")
    private Integer slot;

    @NotBlank(message = "Beschreibung darf nicht leer sein")
    @Schema(description = "Detaillierte Beschreibung der Aktivität", example = "Schule")
    private String description;

    @NotNull(message = "Stunden dürfen nicht null sein")
    @DecimalMin(value = "0.1", message = "Stunden müssen größer als 0 sein")
    @Schema(description = "Stunden, die für die Aktivität aufgewendet wurden", example = "8.0")
    private BigDecimal hours;

    @NotBlank(message = "Sektion darf nicht leer sein")
    @Schema(description = "Abteilung oder Bereich, in dem die Aktivität stattfand", example = "Theorie")
    private String section;
}
