package org.example.javamusicapp.controller.nachweisController.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.javamusicapp.model.enums.EStatus;

import java.util.UUID;

@Data
public class NachweisStatusUpdateRequest {
    @NotNull(message = "Nachweis-ID darf nicht null sein")
    private UUID nachweisId;

    @NotNull(message = "Status darf nicht null sein")
    private EStatus status;

    // Kommentar ist optional, daher keine @NotNull oder @NotBlank
    private String comment;
}
