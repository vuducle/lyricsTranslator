package org.example.javamusicapp.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.javamusicapp.model.enums.EStatus;
import org.example.javamusicapp.model.enums.Weekday;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "nachweis")
public class Nachweis {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @Column(nullable = false)
        private String name;

        private LocalDate datumStart;
        private LocalDate datumEnde;

        private int nummer;

        private String Ausbildungsjahr;

        @Enumerated(EnumType.STRING)
        private EStatus status;

        private String comment;

        @ManyToOne
        @JoinColumn(name = "ausbilder_id")
        private User ausbilder;

        @ManyToOne
        @JoinColumn(name = "azubi_id")
        private User azubi;

        private LocalDate datumAzubi;

        private String signaturAzubi;
        private String signaturAusbilder;

        @JsonManagedReference
        @OneToMany(mappedBy = "nachweis", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Activity> activities = new ArrayList<>();

        // Kopierkonstruktor für Audit-Zwecke
        public Nachweis(Nachweis other) {
                this.id = other.id;
                this.name = other.name;
                this.datumStart = other.datumStart;
                this.datumEnde = other.datumEnde;
                this.nummer = other.nummer;
                this.Ausbildungsjahr = other.Ausbildungsjahr;
                this.status = other.status;
                this.comment = other.comment;
                this.ausbilder = other.ausbilder; // Shallow copy, assuming User is managed
                this.azubi = other.azubi; // Shallow copy, assuming User is managed
                this.datumAzubi = other.datumAzubi;
                this.signaturAzubi = other.signaturAzubi;
                this.signaturAusbilder = other.signaturAusbilder;
                this.activities = new ArrayList<>(); // Neue Liste für Aktivitäten
                for (Activity activity : other.activities) {
                        Activity newActivity = new Activity(activity); // Annahme: Activity hat auch einen Kopierkonstruktor
                        newActivity.setNachweis(this);
                        this.activities.add(newActivity);
                }
        }

        public void addActivity(Activity activity) {
                if (activity == null)
                        return;
                activity.setNachweis(this);
                this.activities.add(activity);
        }


        public BigDecimal totalForDay(Weekday day) {
                return activities.stream()
                                .filter(a -> a.getDay() == day)
                                .map(Activity::getHours)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
}
