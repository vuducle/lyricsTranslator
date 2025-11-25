package org.example.javamusicapp.service;

import lombok.RequiredArgsConstructor;
import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.repository.NachweisRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("nachweisSecurityService")
@RequiredArgsConstructor
public class NachweisSecurityService {

    private final NachweisRepository nachweisRepository;

    public boolean isOwner(Authentication authentication, UUID nachweisId) {
        String username = authentication.getName();
        return nachweisRepository.findById(nachweisId)
                .map(nachweis -> nachweis.getAzubi().getUsername().equals(username))
                .orElse(false);
    }
}
