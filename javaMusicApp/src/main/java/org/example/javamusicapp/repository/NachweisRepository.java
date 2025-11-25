package org.example.javamusicapp.repository;

import org.example.javamusicapp.model.Nachweis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NachweisRepository extends JpaRepository<Nachweis, UUID> {
    List<Nachweis> findAllByAzubiId(UUID azubiId);
}
