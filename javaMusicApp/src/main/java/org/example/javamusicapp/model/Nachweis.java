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

        public void addActivity(Activity activity) {
                if (activity == null)
                        return;
                activity.setNachweis(this);
                this.activities.add(activity);
        }

        public void removeActivity(Activity activity) {
                if (activity == null)
                        return;
                this.activities.remove(activity);
                activity.setNachweis(null);
        }

        public BigDecimal totalForDay(Weekday day) {
                return activities.stream()
                                .filter(a -> a.getDay() == day)
                                .map(Activity::getHours)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
}
