package org.example.javamusicapp.dto.nachweis;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NachweisAuditDto {
    private UUID id;
    private UUID nachweisId;
    private String aktion;
    private LocalDateTime aktionsZeit;
    private String benutzerName;
    private JsonNode alteDaten;
    private JsonNode neueDaten;
}
