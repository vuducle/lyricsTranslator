package org.example.javamusicapp.service;

import lombok.RequiredArgsConstructor;
import org.example.javamusicapp.controller.nachweisController.dto.CreateNachweisRequest;
import org.example.javamusicapp.model.Activity;
import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.model.enums.EStatus;
import org.example.javamusicapp.model.enums.Weekday;
import org.example.javamusicapp.repository.NachweisRepository;
import org.example.javamusicapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NachweisService {

    private final NachweisRepository nachweisRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    @Transactional
    public Nachweis createNachweis(CreateNachweisRequest request, String username) {
        User user = userService.findByUsername(username);
        
        User ausbilder = userRepository.findById(request.getAusbilderId())
                .orElseThrow(() -> new RuntimeException("Ausbilder not found"));

        Nachweis nachweis = new Nachweis();
        nachweis.setName(user.getName());
        nachweis.setDatumStart(request.getDatumStart());
        nachweis.setDatumEnde(request.getDatumEnde());
        nachweis.setNummer(request.getNummer());
        nachweis.setAzubi(user);
        nachweis.setAusbilder(ausbilder);
        nachweis.setStatus(EStatus.IN_BEARBEITUNG);

        if (request.getActivities() == null || request.getActivities().isEmpty()) {
            // Create default activities
            nachweis.addActivity(createActivity(Weekday.MONDAY, 1, "Schule", new BigDecimal("8.0"), "Theorie"));
            nachweis.addActivity(createActivity(Weekday.TUESDAY, 1, "Teambesprechung mit Triesnha Ameilya", new BigDecimal("1.0"), "Meeting"));
            nachweis.addActivity(createActivity(Weekday.TUESDAY, 2, "Coding mit Vergil", new BigDecimal("7.0"), "Entwicklung"));
            nachweis.addActivity(createActivity(Weekday.WEDNESDAY, 1, "Layoutdesign mit Armin Wache", new BigDecimal("4.0"), "Design"));
            nachweis.addActivity(createActivity(Weekday.WEDNESDAY, 2, "Vibe coding mit Vu Quy Le", new BigDecimal("4.0"), "Entwicklung"));
            nachweis.addActivity(createActivity(Weekday.THURSDAY, 1, "Coding mit Vergil", new BigDecimal("8.0"), "Entwicklung"));
            nachweis.addActivity(createActivity(Weekday.FRIDAY, 1, "Coding mit Vergil", new BigDecimal("7.0"), "Entwicklung"));
            nachweis.addActivity(createActivity(Weekday.FRIDAY, 2, "Code Review", new BigDecimal("1.0"), "QA"));
        } else {
            request.getActivities().forEach(activityDTO -> {
                Activity activity = new Activity();
                activity.setDay(activityDTO.getDay());
                activity.setSlot(activityDTO.getSlot());
                activity.setDescription(activityDTO.getDescription());
                activity.setHours(activityDTO.getHours());
                activity.setSection(activityDTO.getSection());
                nachweis.addActivity(activity);
            });
        }

        return nachweisRepository.save(nachweis);
    }

    public List<Nachweis> getNachweiseByAzubiUsername(String username) {
        User azubi = userService.findByUsername(username);
        return nachweisRepository.findAllByAzubiId(azubi.getId());
    }

    private Activity createActivity(Weekday day, Integer slot, String description, BigDecimal hours, String section) {
        Activity activity = new Activity();
        activity.setDay(day);
        activity.setSlot(slot);
        activity.setDescription(description);
        activity.setHours(hours);
        activity.setSection(section);
        return activity;
    }
}
