package org.example.javamusicapp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.javamusicapp.model.enums.Weekday;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "activity")
@Data
@NoArgsConstructor
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nachweis_id", nullable = false)
    private Nachweis nachweis;

    @Enumerated(EnumType.STRING)
    private Weekday day;

    private Integer slot; // 1..5

    @Column(length = 2000)
    private String description;

    private BigDecimal hours;

    private String section;

    // Kopierkonstruktor für Audit-Zwecke
    public Activity(Activity other) {
        this.id = other.id; // ID kopieren, falls benötigt, aber normalerweise bei neuen Objekten null
        this.day = other.day;
        this.slot = other.slot;
        this.description = other.description;
        this.hours = other.hours;
        this.section = other.section;
        // Der Nachweis wird hier NICHT kopiert, da er in der Nachweis-Klasse gesetzt wird,
        // um eine zirkuläre Abhängigkeit zu vermeiden und die korrekte Beziehung sicherzustellen.
    }
}
