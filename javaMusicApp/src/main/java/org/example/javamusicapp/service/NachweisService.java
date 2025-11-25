package org.example.javamusicapp.service;

import lombok.RequiredArgsConstructor;
import org.example.javamusicapp.controller.nachweisController.dto.CreateNachweisRequest;
import org.example.javamusicapp.model.Activity;
import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.model.User;
import org.example.javamusicapp.model.enums.EStatus;
import org.example.javamusicapp.repository.NachweisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NachweisService {

    private final NachweisRepository nachweisRepository;
    private final UserService userService;

    @Transactional
    public Nachweis createNachweis(CreateNachweisRequest request, String username) {
        User user = userService.findByUsername(username);

        Nachweis nachweis = new Nachweis();
        nachweis.setName(user.getName());
        nachweis.setDatumStart(request.getDatumStart());
        nachweis.setDatumEnde(request.getDatumEnde());
        nachweis.setNummer(request.getNummer());
        nachweis.setAzubi(user);
        nachweis.setStatus(EStatus.IN_BEARBEITUNG);

        if (request.getActivities() != null) {
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
}
