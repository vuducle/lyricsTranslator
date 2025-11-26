package org.example.javamusicapp.repository;

import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.model.enums.EStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NachweisRepository extends JpaRepository<Nachweis, UUID> {
    List<Nachweis> findAllByAzubiId(UUID azubiId);
    Page<Nachweis> findAllByAzubiId(UUID azubiId, Pageable pageable);
    Page<Nachweis> findAllByAzubiIdAndStatus(UUID azubiId, EStatus status, Pageable pageable);
}
