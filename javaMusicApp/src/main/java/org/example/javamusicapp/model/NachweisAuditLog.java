package org.example.javamusicapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nachweis_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NachweisAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nachweis_id", nullable = false)
    private UUID nachweisId;

    @Column(name = "aktion", nullable = false)
    private String aktion; // Z.B. "ERSTELLT", "AKTUALISIERT", "GELOESCHT"

    @Column(name = "aktions_zeit", nullable = false)
    private LocalDateTime aktionsZeit;

    @Column(name = "benutzer_name", nullable = false)
    private String benutzerName;

    @Column(name = "alte_daten", columnDefinition = "TEXT")
    private String alteDaten; // JSON-String des Nachweises vor der Änderung

    @Column(name = "neue_daten", columnDefinition = "TEXT")
    private String neueDaten; // JSON-String des Nachweises nach der Änderung
}
